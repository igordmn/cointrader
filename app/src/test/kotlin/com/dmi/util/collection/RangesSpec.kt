package com.dmi.util.collection

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe

class RangesSpec : Spec() {
    init {
        "rangeChunked" {
            (1L..10L).rangeChunked(3) shouldBe listOf(
                    1L..3L,
                    3L..6L,
                    6L..9L,
                    9L..10L
            )
        }
    }
}