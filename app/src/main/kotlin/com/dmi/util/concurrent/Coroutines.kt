package com.dmi.util.concurrent

import kotlinx.coroutines.experimental.async

suspend fun <T, R> Iterable<T>.mapAsync(transform: suspend (T) -> R): Iterable<R> = map { value ->
    async {
        transform(value)
    }
}.map {
    it.await()
}