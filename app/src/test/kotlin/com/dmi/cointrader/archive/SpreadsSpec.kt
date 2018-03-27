package com.dmi.cointrader.archive

import com.dmi.util.restorable.asRestorableSource
import com.dmi.util.test.Spec
import com.dmi.util.test.duration
import com.dmi.util.test.initialValues
import com.dmi.util.test.instant
import io.kotlintest.matchers.shouldBe

class SpreadsSpec: Spec({
    "nextSpread" {
        Trade(instant(22), 0.0, 10.0).nextSpread(previous = Spread(12.0, 11.00)) shouldBe TimeSpread(instant(22), Spread(12.0, 10.0))
        Trade(instant(22), 0.0, 11.4).nextSpread(previous = Spread(12.0, 11.00)) shouldBe TimeSpread(instant(22), Spread(12.0, 11.4))
        Trade(instant(22), 0.0, 11.5).nextSpread(previous = Spread(12.0, 11.00)) shouldBe TimeSpread(instant(22), Spread(11.5, 11.0))
        Trade(instant(22), 0.0, 13.0).nextSpread(previous = Spread(12.0, 11.00)) shouldBe TimeSpread(instant(22), Spread(13.0, 11.0))
    }

    "periodical" - {
        val spreads = listOf(
                TimeSpread(instant(22), Spread(10.0, 1.0)),
                TimeSpread(instant(23), Spread(20.0, 2.0)),
                TimeSpread(instant(24), Spread(30.0, 3.0)),
                TimeSpread(instant(25), Spread(40.0, 4.0)),
                TimeSpread(instant(28), Spread(50.0, 5.0)),
                TimeSpread(instant(32), Spread(60.0, 6.0)),
                TimeSpread(instant(40), Spread(70.0, 7.0))
        ).asRestorableSource()

        "test1" {
            spreads.periodical(PeriodSpace(instant(16), duration(3))).initialValues(11) shouldBe listOf(
                    PeriodSpread(0, Spread(10.0, 1.0)),  // 16
                    PeriodSpread(1, Spread(10.0, 1.0)),  // 19
                    PeriodSpread(2, Spread(10.0, 1.0)),  // 22
                    PeriodSpread(3, Spread(40.0, 4.0)),  // 25
                    PeriodSpread(4, Spread(50.0, 5.0)),  // 28
                    PeriodSpread(5, Spread(50.0, 5.0)),  // 31
                    PeriodSpread(6, Spread(60.0, 6.0)),  // 34
                    PeriodSpread(7, Spread(60.0, 6.0)),  // 37
                    PeriodSpread(8, Spread(70.0, 7.0)),  // 40
                    PeriodSpread(9, Spread(70.0, 7.0)),  // 43
                    PeriodSpread(10, Spread(70.0, 7.0))  // 46
            )
        }

        "test2" {
            spreads.periodical(PeriodSpace(instant(15), duration(3))).initialValues(11) shouldBe listOf(
                    PeriodSpread(0, Spread(10.0, 1.0)),  // 15
                    PeriodSpread(1, Spread(10.0, 1.0)),  // 18
                    PeriodSpread(2, Spread(10.0, 1.0)),  // 21
                    PeriodSpread(3, Spread(30.0, 3.0)),  // 24
                    PeriodSpread(4, Spread(40.0, 4.0)),  // 27
                    PeriodSpread(5, Spread(50.0, 5.0)),  // 30
                    PeriodSpread(6, Spread(60.0, 6.0)),  // 33
                    PeriodSpread(7, Spread(60.0, 6.0)),  // 36
                    PeriodSpread(8, Spread(60.0, 6.0)),  // 39
                    PeriodSpread(9, Spread(70.0, 7.0)),  // 42
                    PeriodSpread(10, Spread(70.0, 7.0))  // 45
            )
        }

        "test3" {
            spreads.periodical(PeriodSpace(instant(17), duration(3))).initialValues(11) shouldBe listOf(
                    PeriodSpread(0, Spread(10.0, 1.0)),  // 17
                    PeriodSpread(1, Spread(10.0, 1.0)),  // 20
                    PeriodSpread(2, Spread(20.0, 2.0)),  // 23
                    PeriodSpread(3, Spread(40.0, 4.0)),  // 26
                    PeriodSpread(4, Spread(50.0, 5.0)),  // 29
                    PeriodSpread(5, Spread(60.0, 6.0)),  // 32
                    PeriodSpread(6, Spread(60.0, 6.0)),  // 35
                    PeriodSpread(7, Spread(60.0, 6.0)),  // 38
                    PeriodSpread(8, Spread(70.0, 7.0)),  // 41
                    PeriodSpread(9, Spread(70.0, 7.0)),  // 44
                    PeriodSpread(10, Spread(70.0, 7.0))  // 47
            )
        }
    }
})