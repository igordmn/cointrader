package com.dmi.util.test

import io.kotlintest.KTestJUnitRunner
import io.kotlintest.TestCase
import io.kotlintest.TestSuite
import kotlinx.coroutines.experimental.runBlocking
import org.junit.runner.RunWith

// io.kotlintest.specs.FreeSpec modification
@RunWith(KTestJUnitRunner::class)
abstract class Spec(init: suspend Spec.() -> Unit = {}) : io.kotlintest.Spec() {
    private var current = rootTestSuite

    init {
        runBlocking {
            init()
        }
    }

    infix operator fun String.minus(init: suspend () -> Unit) {
        val suite = TestSuite(sanitizeSpecName(this))
        current.addNestedSuite(suite)
        val temp = current
        current = suite
        runBlocking {
            init()
        }
        current = temp
    }

    infix operator fun String.invoke(test: suspend () -> Unit): TestCase {
        val testBlocking: () -> Unit = {
            runBlocking {
                test()
            }
        }
        val tc = TestCase(
                suite = current,
                name = sanitizeSpecName(this),
                test = testBlocking,
                config = defaultTestCaseConfig)
        current.addTestCase(tc)
        return tc
    }
}

private fun sanitizeSpecName(name: String) = name.replace("(", " ").replace(")", " ")
