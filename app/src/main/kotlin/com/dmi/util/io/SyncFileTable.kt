package com.dmi.util.io

import com.dmi.util.atom.cached
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.withPrevious
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

interface SyncList<out ITEM> : SuspendList<ITEM> {
    suspend fun sync()
}

fun <T> SyncList<T>.reversed() = object : SyncList<T> {
    suspend override fun size(): Long = this@reversed.size()
    suspend override fun get(range: LongRange) = this@reversed.get(range)
    suspend override fun sync() = this@reversed.sync()
}

interface RestorableSource<STATE : Any, out VALUE> {
    fun restore(state: STATE?): ReceiveChannel<Item<STATE, VALUE>>

    @Serializable
    data class Item<out STATE, out VALUE>(val state: STATE, val value: VALUE) {
        fun toPair() = Pair(state, value)
    }
}

suspend fun <CONFIG : Any, SOURCE_STATE : Any, ITEM> syncFileList(
        file: Path,
        configSerializer: KSerializer<CONFIG>,
        stateSerializer: KSerializer<SOURCE_STATE>,
        itemSerializer: FixedSerializer<ITEM>,
        config: CONFIG,
        source: RestorableSource<SOURCE_STATE, ITEM>,
        bufferSize: Int = 100,
        reloadCount: Int = 0
): SyncList<ITEM> {
    val configStore = NullableFileAtom(file.appendToFileName(".config"), configSerializer).cached()
    val lastInfoStore = NullableFileAtom(file.appendToFileName(".lastId"), SyncInfo.serializer(stateSerializer)).cached()
    val fileArray = FileArray(file.appendToFileName(".array"), itemSerializer)

    val storedConfig = configStore()
    if (storedConfig != config) {
        lastInfoStore.set(null)
        fileArray.clear()
        configStore.set(config)
    }

    fun Long?.plusOneOrZero() = 1 + (this ?: -1)

    return object : SyncList<ITEM> {
        suspend override fun size(): Long = lastInfoStore()?.index.plusOneOrZero()

        suspend override fun get(range: LongRange): List<ITEM> {
            val size = size()
            require(range.start in 0 until size)
            require(range.endInclusive in 0 until size)
            return fileArray.get(range)
        }

        suspend override fun sync() {
            val lastInfo = lastInfoStore()

            var index = lastInfo?.index.plusOneOrZero()
            fileArray.reduceSize(index)

            source.restore(lastInfo?.state).withPrevious(reloadCount).chunked(bufferSize).consumeEach {
                val items = it.map { it.first.value }
                val reloadAfter = it.last().second

                fileArray.append(items)
                if (reloadAfter !=  null) {
                    lastInfoStore.set(SyncInfo(reloadAfter.state, index - reloadCount))
                }

                index++
            }
        }
    }
}

@Serializable
private data class SyncInfo<out STATE>(val state: STATE, val index: Long)