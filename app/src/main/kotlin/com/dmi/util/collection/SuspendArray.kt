package com.dmi.util.collection

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.serialization.Serializable

@Serializable
data class NumIdIndex<out ID : Any>(val num: Long, val id: ID)
data class Indexed<out INDEX, out VALUE>(val index: INDEX, val value: VALUE)

interface SuspendArray<out T> {
    val size: Long

    suspend fun get(range: LongRange): List<T>

    fun channelIndexed(startIndex: Long): ReceiveChannel<Indexed<Long, T>> {
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