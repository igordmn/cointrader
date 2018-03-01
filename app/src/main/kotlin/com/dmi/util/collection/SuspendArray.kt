package com.dmi.util.collection

import com.dmi.util.io.Indexed
import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface SuspendArray<out T> {
    val size: Long

    suspend fun get(range: LongRange): List<T>

    fun channelIndexed(startIndex: Long): ReceiveChannel<Indexed<Long, T>> {
        val bufferSize = 100
    }

    fun <R> map(transform: (T) -> R): SuspendArray<R> {
        val original = this
        return object : SuspendArray<R> {
            override val size: Long = original.size
            override suspend fun get(range: LongRange): List<R> = original.get(range).map(transform)
        }
    }
}