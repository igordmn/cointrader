package com.dmi.cointrader.app.candle

import com.dmi.util.io.FixedSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class Candle(
        val close: Double,
        val high: Double,
        val low: Double,
        val sellPrice: Double = 0.0,
        val buyPrice: Double = 0.0
) {
    init {
        require(high >= low)
        require(close <= high)
        require(close >= low)

        require(sellPrice >= low)
        require(buyPrice >= low)
        require(sellPrice <= high)
        require(buyPrice <= high)
    }

    fun indicator(index: Int) = when (index) {
        0 -> close
        1 -> high
        2 -> low
        else -> throw UnsupportedOperationException()
    }
}

class CandleFixedSerializer : FixedSerializer<Candle> {
    override val itemBytes: Int = 5 * 8

    override fun serialize(item: Candle, data: ByteBuffer) {
        data.putDouble(item.close)
        data.putDouble(item.high)
        data.putDouble(item.low)
        data.putDouble(item.sellPrice)
        data.putDouble(item.buyPrice)
    }

    override fun deserialize(data: ByteBuffer): Candle = Candle(
            data.double,
            data.double,
            data.double,
            data.double,
            data.double
    )
}