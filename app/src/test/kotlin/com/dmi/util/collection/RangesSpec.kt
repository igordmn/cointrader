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

        "IntProgression.size()" {
            (4..10 step 2).size() shouldBe 4  // 4, 6, 8, 10
            (4..9 step 2).size() shouldBe 3   // 4, 6, 8
            (4..4 step 2).size() shouldBe 1   // 4
            (4..10 step 1).size() shouldBe 7  // 4, 5, 6, 7, 8, 9, 10
            (4..9 step 1).size() shouldBe 6   // 4, 5, 6, 7, 8, 9
            (4..4 step 1).size() shouldBe 1   // 4
        }

        "IntProgression.indices()" {
            (4..10 step 2).indices() shouldBe 0..3
            (4..9 step 2).indices() shouldBe 0..2
            (4..10 step 1).indices() shouldBe 0..6
        }

        "IntProgression.slice()" {
            (4..10 step 2).slice(0..0) shouldBe (4..4 step 2)
            (4..10 step 2).slice(0..1) shouldBe (4..6 step 2)
            (4..10 step 2).slice(0..3) shouldBe (4..10 step 2)
            (4..10 step 2).slice(1..2) shouldBe (6..8 step 2)
            (4..10 step 1).slice(1..2) shouldBe (5..6 step 1)
            (4..4 step 2).slice(0..0) shouldBe (4..4 step 2)
        }

        "contains" {
            (3..5 in 3..5) shouldBe true
            (3..5 in 3..6) shouldBe true
            (3..3 in 3..5) shouldBe true
            (5..5 in 3..5) shouldBe true
            (3..4 in 3..5) shouldBe true
            (2..5 in 3..5) shouldBe false
            (3..6 in 3..5) shouldBe false
            (6..7 in 3..5) shouldBe false
            (1..2 in 3..5) shouldBe false
        }

        "coerceIn" {
            (3..7).coerceIn(3..3) shouldBe 3..3
            (3..7).coerceIn(3..10) shouldBe 3..7
            (3..7).coerceIn(4..10) shouldBe 4..7
            (3..7).coerceIn(4..10) shouldBe 4..7
            (3..7).coerceIn(1..3) shouldBe 3..3
            (3..7).coerceIn(1..2) shouldBe 2..2
            (3..7).coerceIn(8..9) shouldBe 7..7
        }
    }
}