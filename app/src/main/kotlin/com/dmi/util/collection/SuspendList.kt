package com.dmi.util.collection

import com.dmi.util.concurrent.flatten
import kotlinx.coroutines.experimental.channels.*

interface SuspendList<out T> {
    suspend fun size(): Long
    suspend fun get(range: LongRange): List<T>

    fun channel(indices: LongRange, bufferSize: Long = 100): ReceiveChannel<T> {
        return indices.chunked(bufferSize).asReceiveChannel().map { get(it) }.flatten()
    }

    fun <R> map(transform: (T) -> R): SuspendList<R> {
        val original = this
        return object : SuspendList<R> {
            override suspend fun size(): Long = original.size()
            override suspend fun get(range: LongRange): List<R> = original.get(range).map(transform)
        }
    }

    suspend fun toList(): List<T> = channel(0 until size()).toList()
}