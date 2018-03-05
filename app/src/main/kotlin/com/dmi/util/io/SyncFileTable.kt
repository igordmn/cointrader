package com.dmi.util.io

import com.dmi.util.collection.Row
import com.dmi.util.collection.Table
import com.dmi.util.collection.rangeChunked
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.withPrevious
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

interface SyncTable<out ITEM> : Table<Long, ITEM> {
    suspend fun sync()
}

suspend fun <CONFIG : Any, SOURCEID : Any, ITEM> syncFileTable(
        file: Path,
        configSerializer: KSerializer<CONFIG>,
        idSerializer: KSerializer<SOURCEID>,
        itemSerializer: FixedSerializer<ITEM>,
        config: CONFIG,
        source: Table<SOURCEID, ITEM>,
        bufferSize: Int = 100,
        reloadCount: Int = 0
): SyncTable<ITEM> {
    val configStore = AtomicFileStore(file.appendToFileName(".config"), configSerializer).cached()
    val lastInfoStore = AtomicFileStore(file.appendToFileName(".lastId"), SyncInfo.serializer(idSerializer)).cached()
    val fileArray = FileArray(file.appendToFileName(".array"), itemSerializer)

    val storedConfig = configStore.readOrNull()
    if (storedConfig != config) {
        lastInfoStore.remove()
        fileArray.clear()
        configStore.write(config)
    }

    fun Long?.plusOneOrZero() = 1 + (this ?: -1)

    return object : SyncTable<ITEM> {
        suspend override fun sync() {
            val lastInfo = lastInfoStore.readOrNull()

            var index = lastInfo?.index.plusOneOrZero()
            fileArray.reduceSize(index)

            source.rowsAfter(lastInfo?.id).withPrevious(reloadCount).chunked(bufferSize).consumeEach {
                val items = it.map { it.first.value }
                val reloadAfter = it.last().second

                fileArray.append(items)
                if (reloadAfter !=  null) {
                    lastInfoStore.write(SyncInfo(reloadAfter.id, index - reloadCount))
                }

                index++
            }
        }

        override fun rowsAfter(id: Long?): ReceiveChannel<Row<Long, ITEM>> = produce {
            val startId = id.plusOneOrZero()
            val size = lastInfoStore.readOrNull()?.index.plusOneOrZero()

            (startId..size).rangeChunked(bufferSize.toLong()).map { range ->
                fileArray.get(range).forEachIndexed { i, it ->
                    send(Row(range.start + i, it))
                }
            }
        }
    }
}

@Serializable
private data class SyncInfo<out ID>(val id: ID, val index: Long)

private interface CachedFileStore<T : Any> {
    fun remove()
    suspend fun write(obj: T)
    fun readOrNull(): T?
}

private suspend fun <T : Any> AtomicFileStore<T>.cached(): CachedFileStore<T> {
    val fileStore = this
    var value = fileStore.readOrNull()
    return object : CachedFileStore<T> {
        override fun remove() = fileStore.remove()

        suspend override fun write(obj: T) {
            fileStore.write(obj)
            value = obj
        }

        override fun readOrNull(): T? = value
    }
}