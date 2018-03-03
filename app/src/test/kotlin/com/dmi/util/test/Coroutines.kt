package com.dmi.util.test

import io.kotlintest.TestCase
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.runBlocking

infix fun Any.blocking(test: suspend () -> Unit): () -> Unit = {
    runBlocking {
        test()
    }
}