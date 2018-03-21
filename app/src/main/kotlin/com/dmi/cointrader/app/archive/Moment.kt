package com.dmi.cointrader.app.archive

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.CandleFixedSerializer
import com.dmi.util.io.FixedListSerializer
import com.dmi.util.io.FixedSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class Moment(val coinIndexToCandle: List<Candle>)

typealias Prices = List<Double>
typealias PriceIncs = List<Double>
typealias PriceIncsBatch = List<PriceIncs>
fun Prices.incs(): PriceIncs = zipWithNext { c, n -> n / c }

class MomentFixedSerializer(size: Int) : FixedSerializer<Moment> {
    private val listSerializer = FixedListSerializer(size, CandleFixedSerializer())
    override val itemBytes: Int = listSerializer.itemBytes
    override fun serialize(item: Moment, data: ByteBuffer) = listSerializer.serialize(item.coinIndexToCandle, data)
    override fun deserialize(data: ByteBuffer): Moment = Moment(listSerializer.deserialize(data))
}

fun Moment.closeAsks(): Prices = coinIndexToCandle.map(Candle::closeAsk)
fun Moment.closeBids(): Prices = coinIndexToCandle.map(Candle::closeBid)
fun Moment.tradeTimeAsks(): Prices = coinIndexToCandle.map(Candle::tradeTimeAsk)
fun Moment.tradeTimeBids(): Prices = coinIndexToCandle.map(Candle::tradeTimeBid)