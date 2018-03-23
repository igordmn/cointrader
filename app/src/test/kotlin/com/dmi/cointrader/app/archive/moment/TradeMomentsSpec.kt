package com.dmi.cointrader.app.archive.moment

import com.dmi.cointrader.app.archive.*
import com.dmi.cointrader.app.archive.Trade
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.collection.SuspendList
import com.dmi.util.test.Spec
import com.dmi.util.test.instant
import com.dmi.util.test.period

class TradeMomentsSpec : Spec({
    val startTime = instant(10)
    val period = period(5)
    val currentTime = MemoryAtom(instant(21))
    val ethTrades = TradeList(
            Trade(instant(15), amount = 2.0, price = 3.5),
            Trade(instant(18), amount = 2.0, price = 2.5),
            Trade(instant(21), amount = 2.0, price = 4.5),
            Trade(instant(25), amount = 2.0, price = 1.5),
            Trade(instant(28), amount = 2.0, price = 6.5)
    )
    val xrpTrades = TradeList(
            Trade(instant(7), amount = 2.0, price = 300.5)
    )
    val ltcTrades = TradeList(
            Trade(instant(8), amount = 2.0, price = 30.5),
            Trade(instant(28), amount = 2.0, price = 60.5)
    )
    val moments = TradeMoments(startTime, period, listOf(ethTrades, xrpTrades, ltcTrades), currentTime)

    "moments at time 21" - {
        "all" {
            moments.restore(null).toList().apply {
                size shouldBe 3
                this[0] shouldBe MomentItem( // 10-15
                        MomentState(0L, listOf(CandleState(0), CandleState(0), CandleState(0))),
                        Moment(listOf(Candle(3.5, 3.5, 3.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
                )
                this[1] shouldBe MomentItem( // 15-20
                        MomentState(1L, listOf(CandleState(1), CandleState(0), CandleState(0))),
                        Moment(listOf(Candle(2.5, 3.5, 2.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
                )
                this[2] shouldBe MomentItem( // 20-25
                        MomentState(2L, listOf(CandleState(2), CandleState(0), CandleState(0))),
                        Moment(listOf(Candle(4.5, 4.5, 4.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
                )
            }
        }

        "restored after first" {
            moments.restore(MomentState(0L, listOf(CandleState(0), CandleState(0), CandleState(0)))).toList().apply {
                size shouldBe 2
                this[0] shouldBe MomentItem( // 15-20
                        MomentState(1L, listOf(CandleState(1), CandleState(0), CandleState(0))),
                        Moment(listOf(Candle(2.5, 3.5, 2.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
                )
                this[1] shouldBe MomentItem( // 20-25
                        MomentState(2L, listOf(CandleState(2), CandleState(0), CandleState(0))),
                        Moment(listOf(Candle(4.5, 4.5, 4.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
                )
            }
        }

        "restored after second" {
            moments.restore(MomentState(1L, listOf(CandleState(1), CandleState(0), CandleState(0)))).toList().apply {
                size shouldBe 1
                this[0] shouldBe MomentItem( // 20-25
                        MomentState(2L, listOf(CandleState(2), CandleState(0), CandleState(0))),
                        Moment(listOf(Candle(4.5, 4.5, 4.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
                )
            }
        }

        "restored after third" {
            moments.restore(MomentState(2L, listOf(CandleState(2), CandleState(0), CandleState(0)))).toList().apply {
                size shouldBe 0
            }
        }
    }

    "moments at time 34" {
        currentTime.set(instant(34))

        "all" {
            val result = moments.restore(null).toList()
            result.size shouldBe 5
            result[0] shouldBe MomentItem( // 10-15
                    MomentState(0L, listOf(CandleState(0), CandleState(0), CandleState(0))),
                    Moment(listOf(Candle(3.5, 3.5, 3.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
            )
            result[1] shouldBe MomentItem( // 15-20
                    MomentState(1L, listOf(CandleState(1), CandleState(0), CandleState(0))),
                    Moment(listOf(Candle(2.5, 3.5, 2.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
            )
            result[2] shouldBe MomentItem( // 20-25
                    MomentState(2L, listOf(CandleState(2), CandleState(0), CandleState(0))),
                    Moment(listOf(Candle(4.5, 4.5, 4.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
            )
            result[3] shouldBe MomentItem( // 25-30
                    MomentState(3L, listOf(CandleState(4), CandleState(0), CandleState(1))),
                    Moment(listOf(Candle(6.5, 6.5, 1.5), Candle(300.5, 300.5, 300.5), Candle(60.5, 60.5, 60.5)))
            )
            result[4] shouldBe MomentItem( // 30-35
                    MomentState(4L, listOf(CandleState(4), CandleState(0), CandleState(1))),
                    Moment(listOf(Candle(6.5, 6.5, 6.5), Candle(300.5, 300.5, 300.5), Candle(60.5, 60.5, 60.5)))
            )
        }

        "restored after first" {
            val result = moments.restore(MomentState(0L, listOf(CandleState(0), CandleState(0), CandleState(0)))).toList()
            result.size shouldBe 4
            result[0] shouldBe MomentItem( // 15-20
                    MomentState(1L, listOf(CandleState(1), CandleState(0), CandleState(0))),
                    Moment(listOf(Candle(2.5, 3.5, 2.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
            )
            result[1] shouldBe MomentItem( // 20-25
                    MomentState(2L, listOf(CandleState(2), CandleState(0), CandleState(0))),
                    Moment(listOf(Candle(4.5, 4.5, 4.5), Candle(300.5, 300.5, 300.5), Candle(30.5, 30.5, 30.5)))
            )
            result[2] shouldBe MomentItem( // 25-30
                    MomentState(3L, listOf(CandleState(4), CandleState(0), CandleState(1))),
                    Moment(listOf(Candle(6.5, 6.5, 1.5), Candle(300.5, 300.5, 300.5), Candle(60.5, 60.5, 60.5)))
            )
            result[3] shouldBe MomentItem( // 30-35
                    MomentState(4L, listOf(CandleState(4), CandleState(0), CandleState(1))),
                    Moment(listOf(Candle(6.5, 6.5, 6.5), Candle(300.5, 300.5, 300.5), Candle(60.5, 60.5, 60.5)))
            )
        }

        "restored after fourth" {
            val result = moments.restore(MomentState(3L, listOf(CandleState(4), CandleState(0), CandleState(1)))).toList()
            result.size shouldBe 1
            result[0] shouldBe MomentItem( // 30-35
                    MomentState(4L, listOf(CandleState(4), CandleState(0), CandleState(1))),
                    Moment(listOf(Candle(6.5, 6.5, 6.5), Candle(300.5, 300.5, 300.5), Candle(60.5, 60.5, 60.5)))
            )
        }

        "restored after fifth" {
            val result = moments.restore(MomentState(4L, listOf(CandleState(4), CandleState(0), CandleState(1)))).toList()
            result.size shouldBe 0
        }
    }
})

private class TradeList(vararg trades: Trade) : SuspendList<Trade> {
    private val trades = trades.toList()

    override suspend fun size(): Long = trades.size.toLong()

    override suspend fun get(range: LongRange): List<Trade> = if (range.isEmpty()) {
        emptyList()
    } else {
        trades.subList(range.start.toInt(), range.endInclusive.toInt() + 1)
    }
}