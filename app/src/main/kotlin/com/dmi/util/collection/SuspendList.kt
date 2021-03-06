package com.dmi.util.collection

import com.dmi.util.concurrent.flatten
import kotlinx.coroutines.experimental.channels.*

interface SuspendList<out T> {
    suspend fun size(): Long
    suspend fun get(range: LongRange): List<T>

    fun channel(indices: LongRange, bufferSize: Long = 10000): ReceiveChannel<T> {
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

fun <T> List<T>.asSuspend() = object : SuspendList<T> {
    override suspend fun size(): Long = size.toLong()
    override suspend fun get(range: LongRange): List<T> = slice(range.toInt())
}

suspend fun <T> SuspendList<T>.memoryCache() = toList().asSuspend()