package com.dmi.cointrader.app.candle

import com.dmi.util.io.FixedSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
data class Candle(
        val closeAsk: Double,
        val closeBid: Double,
        val high: Double,
        val low: Double,
        val tradeTimeAsk: Double,
        val tradeTimeBid: Double
) {

    init {
        require(high >= low)
        require(closeAsk >= low)
        require(closeBid >= low)
        require(tradeTimeAsk >= low)
        require(tradeTimeBid >= low)
        require(closeAsk <= high)
        require(closeBid <= high)
        require(tradeTimeAsk <= high)
        require(tradeTimeBid <= high)
    }

    fun indicator(index: Int) = when (index) {
        0 -> closeAsk
        1 -> closeBid
        2 -> high
        3 -> low
        else -> throw UnsupportedOperationException()
    }
}

class CandleFixedSerializer : FixedSerializer<Candle> {
    override val itemBytes: Int = 6 * 8

    override fun serialize(item: Candle, data: ByteBuffer) {
        data.putDouble(item.closeAsk)
        data.putDouble(item.closeBid)
        data.putDouble(item.high)
        data.putDouble(item.low)
        data.putDouble(item.tradeTimeAsk)
        data.putDouble(item.tradeTimeBid)
    }

    override fun deserialize(data: ByteBuffer): Candle = Candle(
            data.double,
            data.double,
            data.double,
            data.double,
            data.double,
            data.double
    )
}