package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.CandleFixedSerializer
import com.dmi.util.io.FixedListSerializer
import com.dmi.util.io.FixedSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class Moment(val coinIndexToCandle: List<Candle>)

typealias Prices = List<Double>

class MomentFixedSerializer(size: Int) : FixedSerializer<Moment> {
    private val listSerializer = FixedListSerializer(size, CandleFixedSerializer())
    override val itemBytes: Int = listSerializer.itemBytes
    override fun serialize(item: Moment, data: ByteBuffer) = listSerializer.serialize(item.coinIndexToCandle, data)
    override fun deserialize(data: ByteBuffer): Moment = Moment(listSerializer.deserialize(data))
}

fun Moment.prices(): Prices = coinIndexToCandle.map(Candle::close)