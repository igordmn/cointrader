package com.dmi.cointrader.archive

import com.dmi.util.io.FixedSerializer
import com.dmi.util.restorable.RestorableSource
import com.dmi.util.restorable.RestorableSource.Item
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.time.Instant

@Serializable
data class Spread(val ask: Double, val bid: Double)

@Serializable
data class TimeSpread(val time: Instant, val spread: Spread)

@Serializable
data class PeriodSpread(val period: Period, val spread: Spread)

fun Trade.initialSpread() = TimeSpread(
        time = time,
        spread = Spread(ask = price, bid = price)
)

fun Trade.nextSpread(previous: TimeSpread): TimeSpread {
    val isAsk = previous.spread.ask - price <= price - previous.spread.bid
    return TimeSpread(
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
        periodSpace: PeriodSpace
) = object : RestorableSource<PeriodicalState<STATE>, PeriodSpread> {
    override fun initial() = this@periodical.initial().periodical(0)
    override fun restored(state: PeriodicalState<STATE>) = this@periodical.restored(state.lastBefore.state).periodical(state.period + 1, state.lastBefore)

    private fun ReceiveChannel<Item<STATE, TimeSpread>>.periodical(startPeriod: Period, last: Item<STATE, TimeSpread>? = null) = produce {
        consume {
            val it = iterator()
            if (it.hasNext() || last != null) {
                var lastBefore = last ?: it.next()
                periodSequence(startPeriod).forEach { period ->
                    var firstAfter = lastBefore
                    val startTime = periodSpace.timeOf(period)
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

object SpreadFixedSerializer : FixedSerializer<Spread> {
    override val itemBytes: Int = 2 * 8

    override fun serialize(item: Spread, data: ByteBuffer) {
        data.putDouble(item.ask)
        data.putDouble(item.bid)
    }

    override fun deserialize(data: ByteBuffer): Spread = Spread(
            data.double,
            data.double
    )
}