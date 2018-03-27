package com.dmi.util.restorable

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.*

class RestorableSourceSpec : Spec({
    class FibonacciState(val previous1: Int, val previous2: Int)

    val fibonacci = object : RestorableSource<FibonacciState, Int> {
        override fun initial() = restored(FibonacciState(0, 1))
        override fun restored(state: FibonacciState) = generateSequence(next(state), this::next)
                .map { RestorableSource.Item(it, it.previous2) }
                .asReceiveChannel()

        private fun next(state: FibonacciState): FibonacciState {
            val next = state.previous1 + state.previous2
            return FibonacciState(state.previous2, next)
        }
    }

    "fibonacci" {
        fibonacci.initialValues(5) shouldBe listOf(1, 2, 3, 5, 8)
        fibonacci.restoredAfter(0, 5) shouldBe listOf(2, 3, 5, 8, 13)
        fibonacci.restoredAfter(1, 5) shouldBe listOf(3, 5, 8, 13, 21)
    }

    "drop" {
        fibonacci.drop(1).initialValues(5) shouldBe listOf(2, 3, 5, 8, 13)
        fibonacci.drop(1).restoredAfter(0, 5) shouldBe listOf(3, 5, 8, 13, 21)
        fibonacci.drop(1).restoredAfter(1, 5) shouldBe listOf(5, 8, 13, 21, 34)
    }

    "zip" - {
        "single" {
            listOf(fibonacci).zip().initialValues(5) shouldBe listOf(listOf(1), listOf(2), listOf(3), listOf(5), listOf(8))
            listOf(fibonacci).zip().restoredAfter(0, 5) shouldBe listOf(listOf(2), listOf(3), listOf(5), listOf(8), listOf(13))
            listOf(fibonacci).zip().restoredAfter(1, 5) shouldBe listOf(listOf(3), listOf(5), listOf(8), listOf(13), listOf(21))
        }

        "double" {
            listOf(fibonacci, fibonacci.drop(1)).zip().initialValues(5) shouldBe listOf(
                    listOf(1, 2), listOf(2, 3), listOf(3, 5), listOf(5, 8), listOf(8, 13)
            )
            listOf(fibonacci, fibonacci.drop(1)).zip().restoredAfter(0, 5) shouldBe listOf(
                    listOf(2, 3), listOf(3, 5), listOf(5, 8), listOf(8, 13), listOf(13, 21)
            )
            listOf(fibonacci, fibonacci.drop(1)).zip().restoredAfter(1, 5) shouldBe listOf(
                    listOf(3, 5), listOf(5, 8), listOf(8, 13), listOf(13, 21), listOf(21, 34)
            )
        }
    }
})

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.initialValues(count: Int): List<VALUE> {
    return initial().take(count).map { it.value }.toList()
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.restoredAfter(index: Int, count: Int): List<VALUE> {
    return restored(stateAt(index)).take(count).map { it.value }.toList()
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.stateAt(index: Int) = initial().elementAt(index).state