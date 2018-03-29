package com.dmi.util.collection

import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow

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

        "LongProgression.size()" {
            (4L..10L step 2L).size() shouldBe 4L  // 4, 6, 8, 10
            (4L..9L step 2L).size() shouldBe 3L   // 4, 6, 8
            (4L..4L step 2L).size() shouldBe 1L   // 4
            (4L..10L step 1L).size() shouldBe 7L  // 4, 5, 6, 7, 8, 9, 10
            (4L..9L step 1L).size() shouldBe 6L   // 4, 5, 6, 7, 8, 9
            (4L..4L step 1L).size() shouldBe 1L   // 4
        }

        "LongProgression.indices()" {
            (4L..10L step 2L).indices() shouldBe 0L..3L
            (4L..9L step 2L).indices() shouldBe 0L..2L
            (4L..10L step 1L).indices() shouldBe 0L..6L
        }

        "LongProgression.slice()" {
            (4L..10L step 2L).slice(0L..0L) shouldBe (4L..4L step 2L)
            (4L..10L step 2L).slice(0L..1L) shouldBe (4L..6L step 2L)
            (4L..10L step 2L).slice(0L..3L) shouldBe (4L..10L step 2L)
            (4L..10L step 2L).slice(1L..2L) shouldBe (6L..8L step 2L)
            (4L..10L step 1L).slice(1L..2L) shouldBe (5L..6L step 1L)
            (4L..4L step 2L).slice(0L..0L) shouldBe (4L..4L step 2L)
        }

        "contains" {
            (3L..5L in 3L..5L) shouldBe true
            (3L..5L in 3L..6L) shouldBe true
            (3L..3L in 3L..5L) shouldBe true
            (5L..5L in 3L..5L) shouldBe true
            (3L..4L in 3L..5L) shouldBe true
            (2L..5L in 3L..5L) shouldBe false
            (3L..6L in 3L..5L) shouldBe false
            (6L..7L in 3L..5L) shouldBe false
            (1L..2L in 3L..5L) shouldBe false
        }

        "coerceIn" {
            (3L..7L).coerceIn(3L..3L) shouldBe 3L..3L
            (3L..7L).coerceIn(3L..10L) shouldBe 3L..7L
            (3L..7L).coerceIn(4L..10L) shouldBe 4L..7L
            (3L..7L).coerceIn(4L..10L) shouldBe 4L..7L
            (3L..7L).coerceIn(1L..3L) shouldBe 3L..3L
            (3L..7L).coerceIn(1L..2L) shouldBe 2L..2L
            (3L..7L).coerceIn(8L..9L) shouldBe 8L..8L
        }
    }
}