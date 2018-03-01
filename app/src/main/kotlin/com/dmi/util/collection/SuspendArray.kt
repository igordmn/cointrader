package com.dmi.util.collection

import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface SuspendArray<out T> {
    val size: Long

    suspend fun get(range: LongRange): List<T>

    fun channel(startIndex: Long): ReceiveChannel<T> {
        val bufferSize = 100
    }
}