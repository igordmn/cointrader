package com.dmi.util.concurrent

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*

suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): Iterable<R> = map { value ->
    async {
        transform(value)
    }
}.map {
    it.await()
}

fun <T> ReceiveChannel<List<T>>.flatten(): ReceiveChannel<T> = flatMap { it.asReceiveChannel() }

fun <T> ReceiveChannel<T>.chunked(size: Int): ReceiveChannel<List<T>> = produce {
    var chunk = ArrayList<T>(size)
    consumeEach {
        if (chunk.size == size) {
            send(chunk)
            chunk = ArrayList(size)
        }
        chunk.add(it)
    }
    if (chunk.isNotEmpty()) {
        send(chunk)
    }
}