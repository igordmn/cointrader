package com.dmi.cointrader.app.candle

import com.dmi.util.io.FixedSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class Candle(val close: Double, val high: Double, val low: Double) {
    init {
        require(high >= low)
        require(close <= high)
        require(close >= low)
    }
}

class CandleFixedSerializer : FixedSerializer<Candle> {
    override val itemBytes: Int = 3 * 8

    override fun serialize(item: Candle, data: ByteBuffer) {
        data.putDouble(item.close)
        data.putDouble(item.high)
        data.putDouble(item.low)
    }

    override fun deserialize(data: ByteBuffer): Candle = Candle(
            data.double,
            data.double,
            data.double
    )
}