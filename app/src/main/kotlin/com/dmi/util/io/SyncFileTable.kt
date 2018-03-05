package com.dmi.util.io

import com.dmi.util.atom.cached
import com.dmi.util.collection.Row
import com.dmi.util.collection.Table
import com.dmi.util.collection.rangeChunked
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.withPrevious
import kotlinx.coroutines.experimental.channels.*
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
    val configStore = NullableFileAtom(file.appendToFileName(".config"), configSerializer).cached()
    val lastInfoStore = NullableFileAtom(file.appendToFileName(".lastId"), SyncInfo.serializer(idSerializer)).cached()
    val fileArray = FileArray(file.appendToFileName(".array"), itemSerializer)

    val storedConfig = configStore()
    if (storedConfig != config) {
        lastInfoStore.set(null)
        fileArray.clear()
        configStore.set(config)
    }

    fun Long?.plusOneOrZero() = 1 + (this ?: -1)

    return object : SyncTable<ITEM> {
        suspend override fun sync() {
            val lastInfo = lastInfoStore()

            var index = lastInfo?.index.plusOneOrZero()
            fileArray.reduceSize(index)

            source.rowsAfter(lastInfo?.id).withPrevious(reloadCount).chunked(bufferSize).consumeEach {
                val items = it.map { it.first.value }
                val reloadAfter = it.last().second

                fileArray.append(items)
                if (reloadAfter !=  null) {
                    lastInfoStore.set(SyncInfo(reloadAfter.id, index - reloadCount))
                }

                index++
            }
        }

        override fun rowsAfter(id: Long?): ReceiveChannel<Row<Long, ITEM>> = produce {
            val startId = id.plusOneOrZero()
            val size = lastInfoStore()?.index.plusOneOrZero()

            (startId until size).rangeChunked(bufferSize.toLong()).asReceiveChannel().map { range ->
                fileArray.get(range).forEachIndexed { i, it ->
                    send(Row(range.start + i, it))
                }
            }
        }
    }
}

@Serializable
private data class SyncInfo<out ID>(val id: ID, val index: Long)