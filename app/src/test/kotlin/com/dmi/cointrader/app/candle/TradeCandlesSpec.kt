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
        val startTime = instant(17)
        val period = period(4)

        "empty" {
            channelOf<IndexedTrade<Int>>()
                    .candles(startTime, period, 0L..0L)
                    .toList() shouldBe emptyList<TradesCandle<Int>>()
        }

        "single" {
            channelOf(
                    trade(instant(5), 2, 2.5)
            ).candles(startTime, period, 0L..0L).toList() shouldBe listOf(
                    TradesCandle(0, Candle(2.5, 2.5, 2.5), 2)
            )
        }
    }

    private fun instant(millis: Long): Instant = Instant.ofEpochMilli(millis)
    private fun period(millis: Long): Duration = Duration.ofMillis(millis)
    private fun trade(time: Instant, index: Int, price: Double) = IndexedTrade(index, Trade(time, 1.0, price))
}