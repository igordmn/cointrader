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
data class TimeSpread(val time: Instant, val ask: Double, val bid: Double)

@Serializable
data class PeriodSpread(val period: Period, val ask: Double, val bid: Double)

fun Trade.toSpread(previous: TimeSpread?): TimeSpread = if (previous == null) {
    TimeSpread(
            time = time,
            ask = price,
            bid = price
    )
} else {
    val isAsk = previous.ask - price <= price - previous.bid
    TimeSpread(
            time = time,
            ask = if (isAsk) price else previous.ask,
            bid = if (!isAsk) price else previous.bid
    )
}

@Serializable
data class PeriodicalState<out SOURCE_STATE>(val period: Period, val source: SOURCE_STATE)

fun <STATE> RestorableSource<STATE, TimeSpread>.periodical(
        periods: Periods
) = object : RestorableSource<PeriodicalState<STATE>, PeriodSpread> {
    override fun initial() = this@periodical.initial().periodical(Period(0))
    override fun restored(state: PeriodicalState<STATE>) = this@periodical.restored(state.source).periodical(state.period.next())

    private fun ReceiveChannel<Item<STATE, TimeSpread>>.periodical(startPeriod: Period) = produce {
        consume {
            val it = iterator()
            if (it.hasNext()) {
                var lastBefore = it.next()
                periodSequence(startPeriod).forEach { period ->
                    var firstAfter = lastBefore
                    val startTime = periods.startOf(period)
                    while (it.hasNext() && firstAfter.value.time <= startTime) {
                        lastBefore = firstAfter
                        firstAfter = it.next()
                    }
                    val state = PeriodicalState(period, lastBefore.state)
                    val spread = PeriodSpread(period, lastBefore.value.ask, lastBefore.value.bid)
                    send(Item(state, spread))
                }
            }
        }
    }
}