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

@Serializable
data class Spread(val ask: Double, val bid: Double) {
    init {
        require(ask >= bid)
    }
}

@Serializable
data class TimeSpread(
        @Serializable(with = InstantSerializer::class) val time: Instant,
        val spread: Spread)

@Serializable
data class PeriodSpread(val period: Period, val spread: Spread)

@Serializable
class TimeSpreadBillet(
        @Serializable(with = InstantSerializer::class) val time: Instant,
        val lastSell: Double?,
        val lastBuy: Double?,
        val lastIsBuy: Boolean
) {
    fun isReady(): Boolean = lastSell != null && lastBuy != null

    fun build(): TimeSpread {
        val lastSell = lastSell!!
        val lastBuy = lastBuy!!
        return TimeSpread(
                time,
                spread = when {
                    lastSell >= lastBuy -> Spread(lastSell, lastBuy)
                    lastIsBuy -> Spread(lastBuy, lastBuy)
                    else -> Spread(lastSell, lastSell)
                }
        )
    }
}

fun Trade.initialSpreadBillet() = TimeSpreadBillet(
        time = time,
        lastSell = if (!isMakerBuyer) price else null,
        lastBuy = if (isMakerBuyer) price else null,
        lastIsBuy = isMakerBuyer
)

fun Trade.nextSpreadBillet(previous: TimeSpreadBillet) = TimeSpreadBillet(
        time = time,
        lastSell = if (!isMakerBuyer) price else previous.lastSell,
        lastBuy = if (isMakerBuyer) price else previous.lastBuy,
        lastIsBuy = isMakerBuyer
)

fun <SOURCE_STATE> spreadsStateSerializer(source: KSerializer<SOURCE_STATE>) =
        ScanState.serializer(
                source,
                TimeSpreadBillet.serializer()
        )

fun <SOURCE_STATE> RestorableSource<SOURCE_STATE, Trade>.spreads() =
        scan(Trade::initialSpreadBillet, Trade::nextSpreadBillet)
                .dropWhile { !it.isReady() }
                .map { it.build() }

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