package com.dmi.util.io

import com.dmi.util.atom.cached
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.withLongIndex
import com.dmi.util.concurrent.withPrevious
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

interface SyncList<out T> : SuspendList<T> {
    suspend fun sync()

    override fun <R> map(transform: (T) -> R): SyncList<R> = object : SyncList<R> {
        suspend override fun sync() = this@SyncList.sync()
        override suspend fun size(): Long = this@SyncList.size()
        override suspend fun get(range: LongRange): List<R> = this@SyncList.get(range).map(transform)
    }

    interface Log<in T> {
        fun itemsAppended(items: List<T>, indices: LongRange) {}
    }

    class EmptyLog<in T>: Log<T>
}

interface RestorableSource<STATE : Any, out VALUE> {
    fun restore(state: STATE?): ReceiveChannel<Item<STATE, VALUE>>

    @Serializable
    data class Item<out STATE, out VALUE>(val state: STATE, val value: VALUE)
}

suspend fun <CONFIG : Any, SOURCE_STATE : Any, ITEM> syncFileList(
        file: Path,
        configSerializer: KSerializer<CONFIG>,
        stateSerializer: KSerializer<SOURCE_STATE>,
        itemSerializer: FixedSerializer<ITEM>,
        config: CONFIG,
        source: RestorableSource<SOURCE_STATE, ITEM>,
        log: SyncList.Log<ITEM> = SyncList.EmptyLog(),
        bufferSize: Int = 100,
        reloadCount: Int = 0
): SyncList<ITEM> {
    val configStore = NullableFileAtom(file.appendToFileName(".config"), configSerializer).cached()
    val lastInfoStore = NullableFileAtom(file.appendToFileName(".lastInfo"), SyncInfo.serializer(stateSerializer)).cached()
    val fileArray = FileArray(file.appendToFileName(".array"), itemSerializer)

    val storedConfig = configStore()
    if (storedConfig != config) {
        lastInfoStore.set(null)
        fileArray.clear()
        configStore.set(config)
    }

    fun Long?.plusOneOrZero() = 1 + (this ?: -1)

    return object : SyncList<ITEM> {
        suspend override fun size(): Long = fileArray.size

        suspend override fun get(range: LongRange): List<ITEM> {
            val size = size()
            require(range.start in 0 until size)
            require(range.endInclusive in 0 until size)
            return fileArray.get(range)
        }

        suspend override fun sync() {
            val lastInfo = lastInfoStore()

            val startIndex = lastInfo?.index.plusOneOrZero()
            fileArray.truncate(startIndex)

            source
                    .restore(lastInfo?.state)
                    .withLongIndex(startIndex)
                    .withPrevious(reloadCount)
                    .chunked(bufferSize)
                    .consumeEach {
                        val items = it.map {
                            val itemIndexed = it.current
                            itemIndexed.value.value
                        }
                        val reloadAfter = it.last().previous

                        fileArray.append(items)
                        log.itemsAppended(items, it.first().current.index..it.last().current.index)

                        if (reloadAfter != null) {
                            lastInfoStore.set(SyncInfo(reloadAfter.value.state, reloadAfter.index))
                        }
                    }
        }
    }
}

@Serializable
private data class SyncInfo<out STATE>(val state: STATE, val index: Long)