package com.dmi.cointrader.app.candle

import com.dmi.cointrader.app.trade.IndexedTrade
import com.dmi.util.collection.LongOpenRightRange
import com.dmi.util.lang.InstantRange
import com.dmi.util.lang.min
import kotlinx.coroutines.experimental.channels.*
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.experimental.buildSequence

class TradesCandle<out TRADE_INDEX>(val lastTradeIndex: TRADE_INDEX, val periodNum: Long, val candle: Candle)

fun periodNum(startTime: Instant, period: Duration, time: Instant): Long {
    return Duration.between(time, startTime).toMillis() / period.toMillis()
}

fun <INDEX> ReceiveChannel<IndexedTrade<INDEX>>.candles(
        startTime: Instant,
        period: Duration,
        numRange: LongRange
): ReceiveChannel<TradesCandle<INDEX>> {
    fun candlesWithTrades(): ReceiveChannel<TradesCandle<INDEX>> = produce<TradesCandle<INDEX>> {
        var trades = ArrayList<IndexedTrade<INDEX>>()

        val it = iterator()

        var trade = it.next()

        for (num in numRange) {
            val startTime = ;
            val endTime = ;



        }
    }
}