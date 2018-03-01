package com.dmi.cointrader.app.candle

import com.dmi.cointrader.app.nn.CandleId
import com.dmi.cointrader.app.nn.periodIndex
import com.dmi.cointrader.app.trade.IndexedTrade
import com.dmi.util.io.NumIdIndex
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.channels.takeWhile
import java.time.Duration
import java.time.Instant


class TradesCandle<out TRADE_INDEX>(val firstTradeIndex: TRADE_INDEX, val candle: Candle)

fun periodIndex(startTime: Instant, period: Duration, time: Instant): Long {
    return Duration.between(time, startTime).toMillis() / period.toMillis()
}

fun <INDEX> ReceiveChannel<IndexedTrade<INDEX>>.candles(
        startTime: Instant,
        endTime: Instant,
        period: Duration
): ReceiveChannel<TradesCandle<INDEX>> = candlesWithTrades(startTime, endTime, period).candlesWithoutTrades(startTime, endTime, period)

class CandleBuilder(private val startTime: Instant, private val period: Duration) {
    private val trades = ArrayList<TradeItem>()
    private var periodIndex: Long = -1

    fun addAndBuild(trade: TradeItem): CandleItem? {
        val periodIndex = periodIndex(startTime, period, trade.value.time)
        return if (trades.isNotEmpty()) {
            require(periodIndex >= this.periodIndex)
            if (periodIndex == this.periodIndex) {
                trades.add(trade)
                null
            } else {
                val candle = build()
                trades.clear()
                trades.add(trade)
                this.periodIndex = periodIndex
                candle
            }
        } else {
            trades.add(trade)
            this.periodIndex = periodIndex
            null
        }
    }

    fun buildLast(): CandleItem? = if (trades.isNotEmpty()) build() else null

    private fun build() = CandleItem(
            NumIdIndex(
                    periodIndex.apply { require(this >= 0) },
                    CandleId(trades.first().index.id)
            ),
            Candle(
                    trades.last().value.price,
                    trades.maxBy { it.value.price }!!.value.price,
                    trades.minBy { it.value.price }!!.value.price
            )
    )
}

private fun ReceiveChannel<TradeItem>.candlesWithTrades(
        startTime: Instant,
        endTime: Instant,
        period: Duration
): ReceiveChannel<CandleItem> = produce<CandleItem> {
    val candleBuilder = CandleBuilder(startTime, period)
    takeWhile { it.value.time < endTime }.consumeEach {
        val candle = candleBuilder.addAndBuild(it)
        if (candle != null) {
            send(candle)
        }
    }
    val lastCandle = candleBuilder.buildLast()
    if (lastCandle != null) {
        send(lastCandle)
    }
}

private fun ReceiveChannel<CandleItem>.candlesWithoutTrades(
        startTime: Instant,
        endTime: Instant,
        period: Duration
): ReceiveChannel<CandleItem> = produce {
    var last: CandleItem? = null
    val endIndex = periodIndex(startTime, period, endTime)

    consumeEach {
        val startIndex = if (last != null) last!!.index.num + 1 else 0
        require(it.index.num >= startIndex)

        for (i in startIndex until it.index.num) {
            send(CandleItem(NumIdIndex(i, it.index.id), it.value))
        }

        send(it)

        last = it
    }

    last?.let {
        for (i in it.index.num until endIndex) {
            send(CandleItem(NumIdIndex(i, it.index.id), it.value))
        }
    }
}