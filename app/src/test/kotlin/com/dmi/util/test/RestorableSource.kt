package com.dmi.util.test

import com.dmi.util.restorable.RestorableSource
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.elementAt
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.take
import kotlinx.coroutines.experimental.channels.toList

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.checkValues(expected: List<VALUE>) {
    initialValues(expected.size) shouldBe expected
    for (i in 0 until expected.size) {
        try {
            restoredAfter(i, expected.size - i - 1) shouldBe expected.slice(i + 1 until expected.size)
        } catch (e : Throwable) {
            println("Error restoredAfter $i")
            throw e
        }
    }
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.initialValues(size: Int = Int.MAX_VALUE): List<VALUE> {
    return initial().take(size).map { it.value }.toList()
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.restoredAfter(index: Int, size: Int = Int.MAX_VALUE): List<VALUE> {
    return restored(stateAt(index)).take(size).map { it.value }.toList()
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.stateAt(index: Int) = initial().elementAt(index).state