package com.dmi.util.collection

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe

class RangesSpec : Spec() {
    init {
        "rangeChunked" {
            (1L..10L).rangeChunked(3).toList() shouldBe listOf(
                    1L..3L,
                    4L..6L,
                    7L..9L,
                    10L..10L
            )
        }
    }
}