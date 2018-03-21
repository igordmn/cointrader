package com.dmi.cointrader.app.candle

import com.dmi.cointrader.app.archive.IndexedTrade
import com.dmi.cointrader.app.archive.AskBidTrade
import com.dmi.util.concurrent.chunkedBy
import com.dmi.util.concurrent.insert
import com.dmi.util.concurrent.map
import kotlinx.coroutines.experimental.channels.*
import java.time.Duration

data class TradesCandle<out TRADE_INDEX>(val num: Int, val candle: Candle, val lastTrade: AskBidTrade<TRADE_INDEX>)

fun <TRADE_INDEX> ReceiveChannel<AskBidTrade<TRADE_INDEX>>.candles(
        periods: Periods,
        tradeDelay: Duration,
        lastCandle: TradesCandle<TRADE_INDEX>?
): ReceiveChannel<TradesCandle<TRADE_INDEX>> {

//    askBid?.let {
//        require(trade.value.time >= it.trade.value.time)
//    }


    class CandleBillet(val num: Int, val trades: List<AskBidTrade<TRADE_INDEX>>) {
        fun build() = TradesCandle(num, Candle(
                trades.last().value.price,
                trades.maxBy { it.value.price }!!.value.price,
                trades.minBy { it.value.price }!!.value.price
        ), trades.last().tradeIndex)
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
            .map(CandleBillet::build)
}