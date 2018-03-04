package com.dmi.cointrader.app.candle

import com.dmi.cointrader.app.trade.IndexedTrade
import com.dmi.cointrader.app.trade.Trade
import com.dmi.util.test.Spec
import com.dmi.util.test.channelOf
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.toList
import java.time.Duration
import java.time.Instant

class TradeCandlesSpec : Spec() {
    init {
        "empty trades" {
            channelOf<IndexedTrade<Int>>()
                    .candles(instant(17), period(4), 0L..2L)
                    .toList() shouldBe emptyList<TradesCandle<Int>>()
        }

        "single trade" {
            fun trades() = channelOf(
                    trade(instant(7), 200, 2.5)
            )

            trades().candles(instant(17), period(4), 0L..0L).toList() shouldBe listOf(
                    TradesCandle(0, Candle(2.5, 2.5, 2.5), 200)
            )

            trades().candles(instant(0), period(4), 0L..0L).toList() shouldBe listOf(
                    TradesCandle(0, Candle(2.5, 2.5, 2.5), 200)
            )

            trades().candles(instant(7), period(4), 0L..0L).toList() shouldBe listOf(
                    TradesCandle(0, Candle(2.5, 2.5, 2.5), 200)
            )

            trades().candles(instant(3), period(4), 0L..2L).toList() shouldBe listOf(
                    TradesCandle(0, Candle(2.5, 2.5, 2.5), 200),
                    TradesCandle(1, Candle(2.5, 2.5, 2.5), 200),
                    TradesCandle(2, Candle(2.5, 2.5, 2.5), 200)
            )
        }

        "multiple trades" - {
            fun trades() = channelOf(
                    trade(instant(20), 200, 2.5),
                    trade(instant(20), 400, 3.5),
                    trade(instant(21), 600, 1.5),
                    trade(instant(30), 800, 7.5),
                    trade(instant(34), 1000, 8.5),
                    trade(instant(36), 1200, 1.5),
                    trade(instant(38), 1400, 7.5),
                    trade(instant(39), 1600, 2.5),
                    trade(instant(40), 1800, 3.5),
                    trade(instant(55), 2000, 9.5)
            )

            "candles exact after first trade" {
                trades().candles(instant(20), period(4), 0L..10L).toList() shouldBe listOf(
                        TradesCandle(0, Candle(1.5, 3.5, 1.5), 600),   // 20-24
                        TradesCandle(1, Candle(1.5, 1.5, 1.5), 600),   // 24-28
                        TradesCandle(2, Candle(7.5, 7.5, 7.5), 800),   // 28-32
                        TradesCandle(3, Candle(8.5, 8.5, 8.5), 1000),  // 32-36
                        TradesCandle(4, Candle(2.5, 7.5, 1.5), 1600),  // 36-40
                        TradesCandle(5, Candle(3.5, 3.5, 3.5), 1800),  // 40-44
                        TradesCandle(6, Candle(3.5, 3.5, 3.5), 1800),  // 44-48
                        TradesCandle(7, Candle(3.5, 3.5, 3.5), 1800),  // 48-52
                        TradesCandle(8, Candle(9.5, 9.5, 9.5), 2000),  // 52-56
                        TradesCandle(9, Candle(9.5, 9.5, 9.5), 2000),  // 56-60
                        TradesCandle(10, Candle(9.5, 9.5, 9.5), 2000)  // 60-64
                )

                trades().candles(instant(20), period(4), 0L until 0L).toList() shouldBe emptyList<TradesCandle<Int>>()

                trades().candles(instant(20), period(4), 1L until 1L).toList() shouldBe emptyList<TradesCandle<Int>>()

                trades().candles(instant(20), period(4), 0L..0L).toList() shouldBe listOf(
                        TradesCandle(0, Candle(1.5, 3.5, 1.5), 600)   // 20-24
                )

                trades().candles(instant(20), period(4), 0L..1L).toList() shouldBe listOf(
                        TradesCandle(0, Candle(1.5, 3.5, 1.5), 600),   // 20-24
                        TradesCandle(1, Candle(1.5, 1.5, 1.5), 600)    // 24-28
                )

                trades().candles(instant(20), period(4), 0L..2L).toList() shouldBe listOf(
                        TradesCandle(0, Candle(1.5, 3.5, 1.5), 600),   // 20-24
                        TradesCandle(1, Candle(1.5, 1.5, 1.5), 600),   // 24-28
                        TradesCandle(2, Candle(7.5, 7.5, 7.5), 800)    // 28-32
                )

                trades().candles(instant(20), period(4), 1L..2L).toList() shouldBe listOf(
                        TradesCandle(1, Candle(1.5, 1.5, 1.5), 600),   // 24-28
                        TradesCandle(2, Candle(7.5, 7.5, 7.5), 800)    // 28-32
                )

                trades().candles(instant(20), period(4), 2L..2L).toList() shouldBe listOf(
                        TradesCandle(2, Candle(7.5, 7.5, 7.5), 800)    // 28-32
                )

                trades().candles(instant(20), period(4), 20L..20L).toList() shouldBe listOf(
                        TradesCandle(20, Candle(9.5, 9.5, 9.5), 2000)  // 60-64
                )

                trades().candles(instant(20), period(4), 4L..7L).toList() shouldBe listOf(
                        TradesCandle(4, Candle(2.5, 7.5, 1.5), 1600),  // 36-40
                        TradesCandle(5, Candle(3.5, 3.5, 3.5), 1800),  // 40-44
                        TradesCandle(6, Candle(3.5, 3.5, 3.5), 1800),  // 44-48
                        TradesCandle(7, Candle(3.5, 3.5, 3.5), 1800)   // 48-52
                )
            }

            "candles before first trade" {
                trades().candles(instant(19), period(4), 0L..10L).toList() shouldBe listOf(
                        TradesCandle(0, Candle(1.5, 3.5, 1.5), 600),   // 19-23
                        TradesCandle(1, Candle(1.5, 1.5, 1.5), 600),   // 23-27
                        TradesCandle(2, Candle(7.5, 7.5, 7.5), 800),   // 27-31
                        TradesCandle(3, Candle(8.5, 8.5, 8.5), 1000),  // 31-35
                        TradesCandle(4, Candle(7.5, 7.5, 1.5), 1400),  // 35-39
                        TradesCandle(5, Candle(3.5, 3.5, 2.5), 1800),  // 39-43
                        TradesCandle(6, Candle(3.5, 3.5, 3.5), 1800),  // 43-47
                        TradesCandle(7, Candle(3.5, 3.5, 3.5), 1800),  // 47-51
                        TradesCandle(8, Candle(3.5, 3.5, 3.5), 1800),  // 51-55
                        TradesCandle(9, Candle(9.5, 9.5, 9.5), 2000),  // 55-59
                        TradesCandle(10, Candle(9.5, 9.5, 9.5), 2000)  // 59-63
                )
            }

            "candles after middle of trades" {
                trades().candles(instant(24), period(4), 0L..6L).toList() shouldBe listOf(
                        TradesCandle(0, Candle(2.5, 7.5, 1.5), 1600),  // 36-40
                        TradesCandle(1, Candle(3.5, 3.5, 3.5), 1800),  // 40-44
                        TradesCandle(2, Candle(3.5, 3.5, 3.5), 1800),  // 44-48
                        TradesCandle(3, Candle(3.5, 3.5, 3.5), 1800),  // 48-52
                        TradesCandle(4, Candle(9.5, 9.5, 9.5), 2000),  // 52-56
                        TradesCandle(5, Candle(9.5, 9.5, 9.5), 2000),  // 56-60
                        TradesCandle(6, Candle(9.5, 9.5, 9.5), 2000)   // 60-64
                )

                trades().candles(instant(36), period(4), 0L..3L).toList() shouldBe listOf(
                        TradesCandle(0, Candle(1.5, 1.5, 1.5), 600),   // 24-28
                        TradesCandle(1, Candle(7.5, 7.5, 7.5), 800),   // 28-32
                        TradesCandle(2, Candle(8.5, 8.5, 8.5), 1000),  // 32-36
                        TradesCandle(3, Candle(2.5, 7.5, 1.5), 1600)   // 36-40
                )
            }

            "candles after last trade" {
                trades().candles(instant(100), period(4), 0L..2L).toList() shouldBe listOf(
                        TradesCandle(0, Candle(9.5, 9.5, 9.5), 2000),
                        TradesCandle(1, Candle(9.5, 9.5, 9.5), 2000),
                        TradesCandle(2, Candle(9.5, 9.5, 9.5), 2000)
                )

                trades().candles(instant(100), period(4), 2L..2L).toList() shouldBe listOf(
                        TradesCandle(2, Candle(9.5, 9.5, 9.5), 2000)
                )
            }
        }
    }

    private fun instant(millis: Long): Instant = Instant.ofEpochMilli(millis)
    private fun period(millis: Long): Duration = Duration.ofMillis(millis)
    private fun trade(time: Instant, index: Int, price: Double) = IndexedTrade(index, Trade(time, 1.0, price))
}