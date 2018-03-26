package com.dmi.util.collection

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe

class RangesSpec : Spec() {
    init {
        "chunked" {
            (1..10).chunked(3).toList() shouldBe listOf(
                    1..3,
                    4..6,
                    7..9,
                    10..10
            )
        }
    }
}