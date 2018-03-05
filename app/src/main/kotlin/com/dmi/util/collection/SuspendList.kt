package com.dmi.util.collection

import com.dmi.cointrader.app.trade.Trade
import com.dmi.util.io.SyncList
import kotlinx.coroutines.experimental.channels.*

interface SuspendList<out T> {
    suspend fun size(): Long
    suspend fun get(range: LongRange): List<T>

    fun channel(startIndex: Long, bufferSize: Long = 100): ReceiveChannel<T> = produce {
        (startIndex until size()).rangeChunked(bufferSize).asReceiveChannel().map { range ->
            get(range).forEach { it ->
                send(it)
            }
        }
    }

    fun <R> map(transform: (T) -> R): SuspendList<R> {
        val original = this
        return object : SuspendList<R> {
            override suspend fun size(): Long = original.size()
            override suspend fun get(range: LongRange): List<R> = original.get(range).map(transform)
        }
    }

    suspend fun toList(): List<T> = channel(0).toList()
}