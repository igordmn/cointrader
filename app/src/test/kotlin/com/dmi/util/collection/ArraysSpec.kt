package com.dmi.util.collection

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe

class ArraysSpec : Spec({
    "set" - {
        "test1" {
            val array = arrayOf(1, 2, 3, 4, 5)
            array[0..4] = arrayOf(6, 7, 8, 9, 10)
            array shouldBe arrayOf(6, 7, 8, 9, 10)
        }

        "test2" {
            val array = arrayOf(1, 2, 3, 4, 5)
            array[0..0] = arrayOf(6)
            array shouldBe arrayOf(6, 2, 3, 4, 5)
        }

        "test3" {
            val array = arrayOf(1, 2, 3, 4, 5)
            array[2..3] = arrayOf(10, 12)
            array shouldBe arrayOf(1, 2, 10, 12, 5)
        }
    }
})