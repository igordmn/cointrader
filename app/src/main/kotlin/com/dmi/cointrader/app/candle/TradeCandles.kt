package com.dmi.cointrader.app.candle

import com.dmi.cointrader.app.archive.IndexedTrade
import com.dmi.util.concurrent.chunkedBy
import com.dmi.util.concurrent.insert
import com.dmi.util.concurrent.map
import com.dmi.util.lang.DurationSerializer
import com.dmi.util.lang.InstantSerializer
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.times
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

data class TradesCandle<out TRADE_INDEX>(val num: Int, val candle: Candle, val lastTradeIndex: TRADE_INDEX)

fun <TRADE_INDEX> ReceiveChannel<IndexedTrade<TRADE_INDEX>>.candles(
        periods: Periods,
        nums: IntRange
): ReceiveChannel<TradesCandle<TRADE_INDEX>> {
    class CandleBillet(val num: Int, val trades: List<IndexedTrade<TRADE_INDEX>>) {
        fun build() = TradesCandle(num, Candle(
                trades.last().value.price,
                trades.maxBy { it.value.price }!!.value.price,
                trades.minBy { it.value.price }!!.value.price
        ), trades.last().index)
    }

    fun candleNum(trade: IndexedTrade<TRADE_INDEX>) = periods.of(trade.value.time).num

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