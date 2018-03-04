package com.dmi.cointrader.app.candle

import com.dmi.cointrader.app.trade.IndexedTrade
import com.dmi.util.concurrent.chunkedBy
import com.dmi.util.concurrent.insert
import com.dmi.util.concurrent.map
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking
import java.time.Duration
import java.time.Instant

data class TradesCandle<out TRADE_INDEX>(val num: Long, val candle: Candle, val lastTradeIndex: TRADE_INDEX)

fun candleNum(startTime: Instant, period: Duration, time: Instant): Long {
    return Duration.between(startTime, time).toMillis() / period.toMillis()
}

fun <INDEX> ReceiveChannel<IndexedTrade<INDEX>>.candles(
        startTime: Instant,
        period: Duration,
        nums: LongRange
): ReceiveChannel<TradesCandle<INDEX>> {
    data class CandleBillet(val num: Long, val trades: List<IndexedTrade<INDEX>>) {
        fun build() = TradesCandle(num, Candle(
                trades.last().value.price,
                trades.maxBy { it.value.price }!!.value.price,
                trades.minBy { it.value.price }!!.value.price
        ), trades.last().index)
    }

    fun candleNum(trade: IndexedTrade<INDEX>) = candleNum(startTime, period, trade.value.time)

    fun candlesFirst(first: CandleBillet): List<CandleBillet> = (nums.start until first.num).map {
        CandleBillet(it, listOf(first.trades.first()))
    }

    fun candlesLast(last: CandleBillet): List<CandleBillet> = (last.num + 1..nums.endInclusive).map {
        CandleBillet(it, listOf(last.trades.last()))
    }

    fun candlesBetween(previous: CandleBillet, next: CandleBillet) = (previous.num + 1 until next.num).map {
        CandleBillet(it, listOf(previous.trades.last()))
    }

    return this
            .chunkedBy(::candleNum, ::CandleBillet)
            .insert(::candlesFirst, ::candlesBetween, ::candlesLast)
            .dropWhile { it.num < nums.start }
            .takeWhile { it.num <= nums.endInclusive }
            .map(CandleBillet::build)
}