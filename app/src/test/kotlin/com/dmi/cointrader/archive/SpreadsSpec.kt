package com.dmi.cointrader.archive

import com.dmi.util.lang.millis
import com.dmi.util.restorable.asRestorableSource
import com.dmi.util.test.*
import io.kotlintest.matchers.shouldBe

class SpreadsSpec: Spec({
    "trades to spreads" - {
        fun List<Trade>.toSpreads(): List<TimeSpread?> {
            val spreads = ArrayList<TimeSpread?>()
            var spread = this[0].initialSpread()
            spreads.add(spread)
            for (i in 1 until size) {
                spread = this[i].nextSpread(spread)
                spreads.add(spread)
            }
            return spreads
        }

        "artificial trades" {
            listOf(
                    Trade(instant(3), 60.0, 0.0, false),
                    Trade(instant(4), 61.0, 0.0, false),
                    Trade(instant(5), 50.0, 0.0, true),
                    Trade(instant(6), 50.1, 0.0, true),
                    Trade(instant(7), 50.0, 0.0, true),
                    Trade(instant(8), 50.2, 0.0, true),
                    Trade(instant(9), 53.0, 0.0, false),
                    Trade(instant(10), 54.0, 0.0, true),
                    Trade(instant(11), 53.9, 0.0, true),
                    Trade(instant(12), 55.0, 0.0, false),
                    Trade(instant(16), 51.0, 0.0, false),
                    Trade(instant(17), 52.0, 0.0, false),
                    Trade(instant(18), 52.0, 0.0, true),
                    Trade(instant(19), 51.0, 0.0, true),
                    Trade(instant(20), 51.0, 0.0, false)
            ).toSpreads() shouldBe listOf(
                    null,
                    null,
                    TimeSpread(instant(5), Spread(61.0, 50.0)),
                    TimeSpread(instant(6), Spread(61.0, 50.1)),
                    TimeSpread(instant(7), Spread(61.0, 50.0)),
                    TimeSpread(instant(8), Spread(61.0, 50.2)),
                    TimeSpread(instant(9), Spread(53.0, 50.2)),
                    TimeSpread(instant(10), Spread(54.0, 54.0)),
                    TimeSpread(instant(11), Spread(54.0, 53.9)),
                    TimeSpread(instant(12), Spread(55.0, 53.9)),
                    TimeSpread(instant(16), Spread(51.0, 51.0)),
                    TimeSpread(instant(17), Spread(52.0, 51.0)),
                    TimeSpread(instant(18), Spread(52.0, 52.0)),
                    TimeSpread(instant(19), Spread(52.0, 51.0)),
                    TimeSpread(instant(20), Spread(51.0, 51.0))
            )
        }

        "Binance USDT trades" {
            // from https://api.binance.com/api/v1/aggTrades?symbol=BTCUSDT&fromId=0
            
            listOf(
                    Trade(instant(1502942428322L), 4261.48000000, 0.10000000, true),
                    Trade(instant(1502942432285L), 4261.48000000, 1.60000000, true),

                    Trade(instant(1502942432322L), 4261.48000000, 0.07518300, false),
                    Trade(instant(1502942568879L), 4280.56000000, 0.02960000, false),
                    Trade(instant(1502942568887L), 4280.56000000, 0.23147400, false),
                    Trade(instant(1502942628038L), 4261.48000000, 0.00023400, false),
                    Trade(instant(1502942628046L), 4261.48000000, 0.00211300, false),
                    Trade(instant(1502942637667L), 4261.48000000, 0.00966100, false),
                    Trade(instant(1502942647778L), 4261.48000000, 0.14079600, false),
                    Trade(instant(1502943468063L), 4261.48000000, 0.05976000, false),
                    Trade(instant(1502943468063L), 4264.88000000, 0.01569500, false),
                    Trade(instant(1502943540055L), 4261.48000000, 0.00701500, false),
                    Trade(instant(1502943545980L), 4261.48000000, 0.00298500, false),

                    Trade(instant(1502943578118L), 4261.48000000, 0.12963900, true),
                    Trade(instant(1502943578126L), 4261.48000000, 0.11955800, true),
                    Trade(instant(1502943578132L), 4261.48000000, 0.15001400, true),

                    Trade(instant(1502943600073L), 4264.88000000, 0.50829100, false),
                    Trade(instant(1502943600081L), 4264.88000000, 0.45807300, false),
                    Trade(instant(1502943600089L), 4264.88000000, 0.51665000, false),

                    Trade(instant(1502943668410L), 4264.88000000, 0.65146000, true),
                    Trade(instant(1502943668419L), 4264.88000000, 0.06274900, true),
                    Trade(instant(1502943668427L), 4264.88000000, 0.06787000, true),

                    Trade(instant(1502943673349L), 4266.29000000, 0.02033700, false)
            ).toSpreads() shouldBe listOf(
                    null,
                    null,

                    TimeSpread(instant(1502942432322L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502942568879L), Spread(4280.56000000, 4261.48000000)),
                    TimeSpread(instant(1502942568887L), Spread(4280.56000000, 4261.48000000)),
                    TimeSpread(instant(1502942628038L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502942628046L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502942637667L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502942647778L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502943468063L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502943468063L), Spread(4264.88000000, 4261.48000000)),
                    TimeSpread(instant(1502943540055L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502943545980L), Spread(4261.48000000, 4261.48000000)),

                    TimeSpread(instant(1502943578118L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502943578126L), Spread(4261.48000000, 4261.48000000)),
                    TimeSpread(instant(1502943578132L), Spread(4261.48000000, 4261.48000000)),

                    TimeSpread(instant(1502943600073L), Spread(4264.88000000, 4261.48000000)),
                    TimeSpread(instant(1502943600081L), Spread(4264.88000000, 4261.48000000)),
                    TimeSpread(instant(1502943600089L), Spread(4264.88000000, 4261.48000000)),

                    TimeSpread(instant(1502943668410L), Spread(4264.88000000, 4264.88000000)),
                    TimeSpread(instant(1502943668419L), Spread(4264.88000000, 4264.88000000)),
                    TimeSpread(instant(1502943668427L), Spread(4264.88000000, 4264.88000000)),

                    TimeSpread(instant(1502943673349L), Spread(4266.29000000, 4264.88000000))
            )
        }
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