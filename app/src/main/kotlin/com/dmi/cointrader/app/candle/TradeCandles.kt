package com.dmi.cointrader.app.candle

import com.dmi.cointrader.app.trade.IndexedTrade
import com.dmi.util.concurrent.chunkedBy
import com.dmi.util.concurrent.insert
import com.dmi.util.concurrent.map
import kotlinx.coroutines.experimental.channels.*
import java.time.Duration
import java.time.Instant

class TradesCandle<out TRADE_INDEX>(val lastTradeIndex: TRADE_INDEX, val num: Long, val candle: Candle)

fun candleNum(startTime: Instant, period: Duration, time: Instant): Long {
    return Duration.between(time, startTime).toMillis() / period.toMillis()
}

fun <INDEX> ReceiveChannel<IndexedTrade<INDEX>>.candles(
        startTime: Instant,
        period: Duration,
        nums: LongRange
): ReceiveChannel<TradesCandle<INDEX>> {
    class CandleBillet(val num: Long, val trades: List<IndexedTrade<INDEX>>) {
        fun build() = TradesCandle(trades.last().index, num, Candle(
                trades.last().value.price,
                trades.maxBy { it.value.price }!!.value.price,
                trades.minBy { it.value.price }!!.value.price
        ))
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