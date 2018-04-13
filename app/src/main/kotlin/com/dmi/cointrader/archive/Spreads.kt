package com.dmi.cointrader.archive

import com.dmi.util.io.FixedSerializer
import com.dmi.util.lang.InstantSerializer
import com.dmi.util.restorable.*
import com.dmi.util.restorable.RestorableSource.Item
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.time.Instant
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

@Serializable
data class Spread(val ask: Double, val bid: Double) {
    init {
        require(ask >= bid)
    }
}

@Serializable
data class TimeSpread(
        @Serializable(with = InstantSerializer::class) val time: Instant,
        val spread: Spread
)

@Serializable
data class PeriodSpread(val period: Period, val spread: Spread)

fun Trade.initialSpread() = TimeSpread(time, Spread(price, price))

fun Trade.nextSpread(previous: TimeSpread): TimeSpread {
    val lastAsk: Double?
    val lastBid: Double?
    if (isMakerBuyer) {
        lastBid = price
        lastAsk = if (price > previous.spread.ask) {
            price
        } else {
            previous.spread.ask
        }
    } else {
        lastAsk = price
        lastBid = if (price < previous.spread.bid) {
            price
        } else {
            previous.spread.bid
        }
    }

    return TimeSpread(time, Spread(lastAsk, lastBid))
}

fun <SOURCE_STATE> spreadsStateSerializer(source: KSerializer<SOURCE_STATE>) =
        ScanState.serializer(
                source,
                TimeSpread.serializer()
        )

fun <SOURCE_STATE> RestorableSource<SOURCE_STATE, Trade>.spreads() = scan(Trade::initialSpread, Trade::nextSpread)

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
            if (last != null || it.hasNext()) {
                var lastBefore = last ?: it.next()
                var item = lastBefore
                periodSequence(startPeriod).forEach { period ->
                    val time = periodSpace.timeOf(period)

                    while (item.value.time <= time) {
                        lastBefore = item
                        if (it.hasNext()) {
                            item = it.next()
                        } else {
                            break
                        }
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