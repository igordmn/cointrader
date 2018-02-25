package com.dmi.util.concurrent

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.flatMap

suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): Iterable<R> = map { value ->
    async {
        transform(value)
    }
}.map {
    it.await()
}

fun <T> ReceiveChannel<List<T>>.flatten(): ReceiveChannel<T> = flatMap { it.asReceiveChannel() }