package com.dmi.util.collection

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.serialization.Serializable

@Serializable
data class Row<out ID, out VALUE>(val id: ID, val value: VALUE) {
    fun toPair() = Pair(id, value)
}

interface SuspendArray<out T> {
    val size: Long

    suspend fun get(range: LongRange): List<T>

    fun channelIndexed(startIndex: Long): ReceiveChannel<Row<Long, T>> {
        val bufferSize = 100
        TODO()
    }

    fun <R> map(transform: (T) -> R): SuspendArray<R> {
        val original = this
        return object : SuspendArray<R> {
            override val size: Long = original.size
            override suspend fun get(range: LongRange): List<R> = original.get(range).map(transform)
        }
    }
}

interface Table<ID: Any, out VALUE> {
    fun rowsAfter(id: ID?): ReceiveChannel<Row<ID, VALUE>>
}