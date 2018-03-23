package com.dmi.cointrader.app.archive

import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.Periods
import com.dmi.cointrader.app.candle.periodSequence
import com.dmi.util.restorable.RestorableSource
import com.dmi.util.restorable.RestorableSource.Item
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Spread(val ask: Double, val bid: Double)

@Serializable
data class TimeSpread(val time: Instant, val spread: Spread)

@Serializable
data class PeriodSpread(val period: Period, val spread: Spread)

fun Trade.toSpread(previous: TimeSpread?): TimeSpread = if (previous == null) {
    TimeSpread(
            time = time,
            spread = Spread(ask = price, bid = price)
    )
} else {
    val isAsk = previous.spread.ask - price <= price - previous.spread.bid
    TimeSpread(
            time = time,
            spread = Spread(
                ask = if (isAsk) price else previous.spread.ask,
                bid = if (!isAsk) price else previous.spread.bid
            )
    )
}

@Serializable
data class PeriodicalState<out SOURCE_STATE>(val period: Period, val lastBefore: Item<SOURCE_STATE, TimeSpread>)

fun <STATE> RestorableSource<STATE, TimeSpread>.periodical(
        periods: Periods
) = object : RestorableSource<PeriodicalState<STATE>, PeriodSpread> {
    override fun initial() = this@periodical.initial().periodical(Period(0))
    override fun restored(state: PeriodicalState<STATE>) = this@periodical.restored(state.lastBefore.state).periodical(state.period.next(), state.lastBefore)

    private fun ReceiveChannel<Item<STATE, TimeSpread>>.periodical(startPeriod: Period, last: Item<STATE, TimeSpread>? = null) = produce {
        consume {
            val it = iterator()
            if (it.hasNext() || last != null) {
                var lastBefore = last ?: it.next()
                periodSequence(startPeriod).forEach { period ->
                    var firstAfter = lastBefore
                    val startTime = periods.timeOf(period)
                    while (it.hasNext() && firstAfter.value.time <= startTime) {
                        lastBefore = firstAfter
                        firstAfter = it.next()
                    }
                    val state = PeriodicalState(period, lastBefore)
                    val spread = PeriodSpread(period, lastBefore.value.spread)
                    send(Item(state, spread))
                }
            }
        }
    }
}