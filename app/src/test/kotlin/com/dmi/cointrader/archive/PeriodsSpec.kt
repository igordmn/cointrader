package com.dmi.cointrader.archive

import com.dmi.util.test.Spec
import com.dmi.util.test.duration
import com.dmi.util.test.instant
import io.kotlintest.matchers.shouldBe

class PeriodsSpec : Spec({
    val space = PeriodSpace(instant(21), duration(3))

    "floor" {
        space.floor(instant(17)) shouldBe -2
        space.floor(instant(18)) shouldBe -1
        space.floor(instant(19)) shouldBe -1
        space.floor(instant(20)) shouldBe -1
        space.floor(instant(21)) shouldBe 0
        space.floor(instant(22)) shouldBe 0
        space.floor(instant(23)) shouldBe 0
        space.floor(instant(24)) shouldBe 1
        space.floor(instant(25)) shouldBe 1
        space.floor(instant(26)) shouldBe 1
        space.floor(instant(27)) shouldBe 2
    }

    "ceil" {
        space.ceil(instant(18)) shouldBe -1
        space.ceil(instant(19)) shouldBe 0
        space.ceil(instant(20)) shouldBe 0
        space.ceil(instant(21)) shouldBe 0
        space.ceil(instant(22)) shouldBe 1
        space.ceil(instant(23)) shouldBe 1
        space.ceil(instant(24)) shouldBe 1
        space.ceil(instant(25)) shouldBe 2
    }

    "timeOf" {
        space.timeOf(-1) shouldBe instant(18)
        space.timeOf(0) shouldBe instant(21)
        space.timeOf(1) shouldBe instant(24)
        space.timeOf(2) shouldBe instant(27)
    }

    "times" {
        space * 2 shouldBe PeriodSpace(instant(21), duration(6))
    }

    "periodSequence" {
        periodSequence(0).take(2).toList() shouldBe listOf(0, 1)
        periodSequence(1).take(2).toList() shouldBe listOf(1, 2)
    }

    "periods" {
        (instant(21)..instant(23)).periods(space) shouldBe 0..0
        (instant(21)..instant(24)).periods(space) shouldBe 0..1
        (instant(20)..instant(23)).periods(space) shouldBe 0..0
        (instant(22)..instant(24)).periods(space) shouldBe 1..1
        (instant(22)..instant(26)).periods(space) shouldBe 1..1
        (instant(22)..instant(27)).periods(space) shouldBe 1..2
    }
})