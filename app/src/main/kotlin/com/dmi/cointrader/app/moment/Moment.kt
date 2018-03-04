package com.dmi.cointrader.app.moment

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.CandleFixedSerializer
import com.dmi.util.io.FixedListSerializer
import com.dmi.util.io.FixedSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class Moment(val coinIndexToCandle: List<Candle>)

class MomentFixedSerializer(size: Int) : FixedSerializer<Moment> {
    private val listSerializer = FixedListSerializer(size, CandleFixedSerializer())
    override val itemBytes: Int = listSerializer.itemBytes
    override fun serialize(item: Moment, data: ByteBuffer) = listSerializer.serialize(item.coinIndexToCandle, data)
    override fun deserialize(data: ByteBuffer): Moment = Moment(listSerializer.deserialize(data))
}