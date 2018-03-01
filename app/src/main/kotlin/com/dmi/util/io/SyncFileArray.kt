package com.dmi.util.io

import com.dmi.util.collection.SuspendArray
import com.dmi.util.concurrent.chunked
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.serialization.KSerializer
import java.nio.file.Path

interface IdentitySource<out CONFIG : Any, ITEMID : Any, out ITEM> {
    val config: CONFIG
    fun getNew(lastIndex: Index<ITEMID>?): ReceiveChannel<Item<ITEMID, ITEM>>
    data class Index<out ID : Any>(val num: Long, val id: ID)
    data class Item<out ID : Any, out VALUE>(val index: Index<ID>, val value: VALUE)
}

class SyncFileArray<in CONFIG : Any, ITEMID : Any, ITEM>(
        file: Path,
        configSerializer: KSerializer<CONFIG>,
        idSerializer: KSerializer<ITEMID>,
        itemSerializer: FixedSerializer<ITEM>,
        private val bufferSize: Int = 100
) : SuspendArray<ITEM> {
    private val configStore = AtomicFileStore(file.appendToFileName(".config"), configSerializer)
    private val indexSerializer: KSerializer<IdentitySource.Index<ITEMID>> = TODO()
    private val lastIndexStore = AtomicFileStore(file.appendToFileName(".lastIndex"), indexSerializer)
    private val fileArray = FileArray(file.appendToFileName(".array"), itemSerializer)

    override val size: Long get() = fileArray.size
    override suspend fun get(range: LongRange): List<ITEM> = fileArray.get(range)

    suspend fun syncWith(source: IdentitySource<CONFIG, ITEMID, ITEM>) {
        val config = configStore.readOrNull()
        if (config != source.config) {
            lastIndexStore.remove()
            fileArray.clear()
            configStore.write(source.config)
        }

        val lastIndex = lastIndexStore.readOrNull()

        var isFirst = true
        source.getNew(lastIndex).chunked(bufferSize).consumeEach {
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