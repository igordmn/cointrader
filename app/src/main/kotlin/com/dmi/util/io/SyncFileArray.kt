package com.dmi.util.io

import com.dmi.util.collection.SuspendArray
import com.dmi.util.concurrent.chunked
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.serialization.KSerializer
import java.nio.file.Path

data class NumIdIndex<out ID : Any>(val num: Long, val id: ID)
data class IndexedItem<out INDEX, out VALUE>(val index: INDEX, val value: VALUE)

interface IdentitySource<out CONFIG : Any, INDEX : NumIdIndex<*>, out ITEM> {
    val config: CONFIG
    fun after(lastIndex: INDEX?): ReceiveChannel<IndexedItem<INDEX, ITEM>>
}

class SyncFileArray<in CONFIG : Any, INDEX : NumIdIndex<*>, ITEM>(
        file: Path,
        configSerializer: KSerializer<CONFIG>,
        indexSerializer: KSerializer<INDEX>,
        itemSerializer: FixedSerializer<ITEM>,
        private val bufferSize: Int = 100
) : SuspendArray<ITEM> {
    private val configStore = AtomicFileStore(file.appendToFileName(".config"), configSerializer)
    private val lastIndexStore = AtomicFileStore(file.appendToFileName(".lastIndex"), indexSerializer)
    private val fileArray = FileArray(file.appendToFileName(".array"), itemSerializer)

    override val size: Long get() = fileArray.size
    override suspend fun get(range: LongRange): List<ITEM> = fileArray.get(range)

    suspend fun syncWith(source: IdentitySource<CONFIG, INDEX, ITEM>) {
        val config = configStore.readOrNull()
        if (config != source.config) {
            lastIndexStore.remove()
            fileArray.clear()
            configStore.write(source.config)
        }

        val lastIndex = lastIndexStore.readOrNull()

        var isFirst = true
        source.after(lastIndex).chunked(bufferSize).consumeEach {
            val index = it.last().index
            val items = it.map { it.value }

            if (isFirst) {
                fileArray.reduceSize(index.num)
                isFirst = false
            }

            require(index.num == fileArray.size + items.size - 1)
            fileArray.append(items)
            lastIndexStore.write(index)
        }
    }
}