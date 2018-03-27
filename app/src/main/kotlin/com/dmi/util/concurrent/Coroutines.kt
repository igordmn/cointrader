package com.dmi.util.concurrent

import kotlinx.coroutines.experimental.async

fun <T> suspend(action: suspend () -> T): suspend () -> T = action
fun <T, P> suspend(action: suspend (P) -> T): suspend (P) -> T = action

suspend fun <T> Iterable<T>.forEachAsync(action: suspend (T) -> Unit) = map { value ->
    async {
        action(value)
    }
}.forEach {
    it.await()
}