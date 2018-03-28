package com.dmi.cointrader.archive

import com.dmi.util.lang.millis
import com.dmi.util.restorable.asRestorableSource
import com.dmi.util.test.*

class SpreadsSpec: Spec({
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
            spreads.periodical(PeriodSpace(instant(16), millis(3))).checkValues(listOf(
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
            ))
        }

        "test2" {
            spreads.periodical(PeriodSpace(instant(15), millis(3))).checkValues(listOf(
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
            ))
        }

        "test3" {
            spreads.periodical(PeriodSpace(instant(17), millis(3))).checkValues(listOf(
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
            ))
        }

        "test4" {
            spreads.periodical(PeriodSpace(instant(22), millis(3))).checkValues(listOf(
                    PeriodSpread(0, Spread(10.0, 1.0)),  // 22
                    PeriodSpread(1, Spread(40.0, 4.0)),  // 25
                    PeriodSpread(2, Spread(50.0, 5.0)),  // 28
                    PeriodSpread(3, Spread(50.0, 5.0))   // 31
            ))
        }

        "test5" {
            spreads.periodical(PeriodSpace(instant(26), millis(3))).checkValues(listOf(
                    PeriodSpread(0, Spread(40.0, 4.0)),  // 26
                    PeriodSpread(1, Spread(50.0, 5.0)),  // 29
                    PeriodSpread(2, Spread(60.0, 6.0)),  // 32
                    PeriodSpread(3, Spread(60.0, 6.0))   // 35
            ))
        }

        "test6" {
            spreads.periodical(PeriodSpace(instant(39), millis(3))).checkValues(listOf(
                    PeriodSpread(0, Spread(60.0, 6.0)),  // 39
                    PeriodSpread(1, Spread(70.0, 7.0)),  // 42
                    PeriodSpread(2, Spread(70.0, 7.0))   // 45
            ))
        }

        "test7" {
            spreads.periodical(PeriodSpace(instant(40), millis(3))).checkValues(listOf(
                    PeriodSpread(0, Spread(70.0, 7.0)),
                    PeriodSpread(1, Spread(70.0, 7.0)),
                    PeriodSpread(2, Spread(70.0, 7.0))
            ))
        }

        "test8" {
            spreads.periodical(PeriodSpace(instant(41), millis(3))).checkValues(listOf(
                    PeriodSpread(0, Spread(70.0, 7.0)),
                    PeriodSpread(1, Spread(70.0, 7.0)),
                    PeriodSpread(2, Spread(70.0, 7.0))
            ))
        }
    }
})