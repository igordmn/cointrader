package com.dmi.util.restorable

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.*

class RestorableSourceSpec : Spec({
    class FibonacciState(val previous1: Int, val previous2: Int)

    fun fibonacci(max: Int) = object : RestorableSource<FibonacciState, Int> {
        override fun initial() = restored(FibonacciState(0, 1))
        override fun restored(state: FibonacciState) = generateSequence(next(state), this::next)
                .map { RestorableSource.Item(it, it.previous2) }
                .takeWhile { it.value <= max }
                .asReceiveChannel()

        private fun next(state: FibonacciState): FibonacciState {
            val next = state.previous1 + state.previous2
            return FibonacciState(state.previous2, next)
        }
    }
    
    fun fibonacci() = fibonacci(max = 8)

    "fibonacci" {
        fibonacci().initialValues() shouldBe listOf(1, 2, 3, 5, 8)
        fibonacci().restoredAfter(0) shouldBe listOf(2, 3, 5, 8)
        fibonacci().restoredAfter(1) shouldBe listOf(3, 5, 8)
        fibonacci().restoredAfter(3) shouldBe listOf(8)
        fibonacci().restoredAfter(4) shouldBe emptyList<Int>()
    }

    "drop" {
        fibonacci().drop(1).initialValues() shouldBe listOf(2, 3, 5, 8)
        fibonacci().drop(1).restoredAfter(0) shouldBe listOf(3, 5, 8)
        fibonacci().drop(1).restoredAfter(1) shouldBe listOf(5, 8)
        fibonacci().drop(1).restoredAfter(2) shouldBe listOf(8)
        fibonacci().drop(1).restoredAfter(3) shouldBe emptyList<Int>()
    }

    "zip" - {
        "single" {
            listOf(fibonacci()).zip().initialValues() shouldBe listOf(listOf(1), listOf(2), listOf(3), listOf(5), listOf(8))
            listOf(fibonacci()).zip().restoredAfter(0) shouldBe listOf(listOf(2), listOf(3), listOf(5), listOf(8))
            listOf(fibonacci()).zip().restoredAfter(1) shouldBe listOf(listOf(3), listOf(5), listOf(8))
        }

        "double" {
            val source = listOf(fibonacci(), fibonacci().drop(1))
            source.zip().initialValues() shouldBe listOf(
                    listOf(1, 2), listOf(2, 3), listOf(3, 5), listOf(5, 8)
            )
            source.zip().restoredAfter(0) shouldBe listOf(
                    listOf(2, 3), listOf(3, 5), listOf(5, 8)
            )
            source.zip().restoredAfter(1) shouldBe listOf(
                    listOf(3, 5), listOf(5, 8)
            )
            source.zip().restoredAfter(2) shouldBe listOf(
                    listOf(5, 8)
            )
            source.zip().restoredAfter(3) shouldBe emptyList<List<Int>>()
        }
    }
    
    "scan" {
        fun initial(value: Int) = value * 0.1
        fun operation(value: Int, acc: Double) = acc + value
        val source = fibonacci().scan(::initial, ::operation)
        source.initialValues() shouldBe listOf(0.1, 2.1, 5.1, 10.1, 18.1)
        source.restoredAfter(0) shouldBe listOf(2.1, 5.1, 10.1, 18.1)
        source.restoredAfter(1) shouldBe listOf(5.1, 10.1, 18.1)
        source.restoredAfter(2) shouldBe listOf(10.1, 18.1)
        source.restoredAfter(3) shouldBe listOf(18.1)
        source.restoredAfter(4) shouldBe emptyList<Int>()
    }
})

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.initialValues(): List<VALUE> {
    return initial().map { it.value }.toList()
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.restoredAfter(index: Int): List<VALUE> {
    return restored(stateAt(index)).map { it.value }.toList()
}

suspend fun <STATE, VALUE> RestorableSource<STATE, VALUE>.stateAt(index: Int) = initial().elementAt(index).state