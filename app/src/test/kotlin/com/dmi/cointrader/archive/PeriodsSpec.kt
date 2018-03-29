package com.dmi.cointrader.archive

import com.dmi.util.lang.millis
import com.dmi.util.test.Spec
import com.dmi.util.test.duration
import com.dmi.util.test.instant
import io.kotlintest.matchers.shouldBe

class PeriodsSpec : Spec({
    val space = PeriodSpace(instant(21), millis(3))

    "floor" {
        space.floor(instant(17)) shouldBe -2L
        space.floor(instant(18)) shouldBe -1L
        space.floor(instant(19)) shouldBe -1L
        space.floor(instant(20)) shouldBe -1L
        space.floor(instant(21)) shouldBe 0L
        space.floor(instant(22)) shouldBe 0L
        space.floor(instant(23)) shouldBe 0L
        space.floor(instant(24)) shouldBe 1L
        space.floor(instant(25)) shouldBe 1L
        space.floor(instant(26)) shouldBe 1L
        space.floor(instant(27)) shouldBe 2L
    }

    "ceil" {
        space.ceil(instant(18)) shouldBe -1L
        space.ceil(instant(19)) shouldBe 0L
        space.ceil(instant(20)) shouldBe 0L
        space.ceil(instant(21)) shouldBe 0L
        space.ceil(instant(22)) shouldBe 1L
        space.ceil(instant(23)) shouldBe 1L
        space.ceil(instant(24)) shouldBe 1L
        space.ceil(instant(25)) shouldBe 2L
    }

    "timeOf" {
        space.timeOf(-1) shouldBe instant(18)
        space.timeOf(0) shouldBe instant(21)
        space.timeOf(1) shouldBe instant(24)
        space.timeOf(2) shouldBe instant(27)
    }

    "times" {
        space * 2 shouldBe PeriodSpace(instant(21), millis(6))
    }

    "periodSequence" {
        periodSequence(0).take(2).toList() shouldBe listOf(0L, 1L)
        periodSequence(1).take(2).toList() shouldBe listOf(1L, 2L)
    }

    "periods" {
        (instant(21)..instant(23)).periods(space) shouldBe 0L..0L
        (instant(21)..instant(24)).periods(space) shouldBe 0L..1L
        (instant(20)..instant(23)).periods(space) shouldBe 0L..0L
        (instant(22)..instant(24)).periods(space) shouldBe 1L..1L
        (instant(22)..instant(26)).periods(space) shouldBe 1L..1L
        (instant(22)..instant(27)).periods(space) shouldBe 1L..2L
    }

    "tradePeriods" {
        (0L..9L).tradePeriods(tradeSize = 3).toList() shouldBe listOf(0L, 3L, 6L, 9L)
        (0L..8L).tradePeriods(tradeSize = 3).toList() shouldBe listOf(0L, 3L, 6L)
        (1L..8L).tradePeriods(tradeSize = 3).toList() shouldBe listOf(3L, 6L)
        (3L..6L).tradePeriods(tradeSize = 3).toList() shouldBe listOf(3L, 6L)
        (4L..6L).tradePeriods(tradeSize = 3).toList() shouldBe listOf(6L)
        (3L..5L).tradePeriods(tradeSize = 3).toList() shouldBe listOf(3L)
        (4L..5L).tradePeriods(tradeSize = 3).toList() shouldBe emptyList<Int>()
    }

    "nextTradePeriod" {
        0L.nextTradePeriod(tradeSize = 3) shouldBe 3L
        2L.nextTradePeriod(tradeSize = 3) shouldBe 3L
        3L.nextTradePeriod(tradeSize = 3) shouldBe 6L
        5L.nextTradePeriod(tradeSize = 3) shouldBe 6L
        6L.nextTradePeriod(tradeSize = 3) shouldBe 9L
    }
})