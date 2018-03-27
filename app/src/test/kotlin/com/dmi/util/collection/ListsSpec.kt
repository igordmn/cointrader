package com.dmi.util.collection

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe

class ListsSpec : Spec({
    "zip" - {
        "single list" {
            listOf(
                    listOf(1, 2, 3)
            ).zip() shouldBe listOf(listOf(1), listOf(2), listOf(3))
        }

        "two lists same size" {
            listOf(
                    listOf(1, 2, 3),
                    listOf(4, 5, 6)
            ).zip() shouldBe listOf(listOf(1, 4), listOf(2, 5), listOf(3, 6))
        }

        "three lists different size" {
            listOf(
                    listOf(1, 2, 3),
                    listOf(4, 5, 6),
                    listOf(2, 2)
            ).zip() shouldBe listOf(listOf(1, 4, 2), listOf(2, 5, 2))
        }
    }
})