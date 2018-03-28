package com.dmi.cointrader.archive

import com.dmi.util.io.FixedSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.time.Instant

@Serializable
data class Trade(val time: Instant, val price: Double, val amount: Double, val isBuyerMaker: Boolean) {
    fun reverse() = Trade(time, 1.0 / price, amount * price, !isBuyerMaker)
}

object TradeFixedSerializer : FixedSerializer<Trade> {
    override val itemBytes: Int = 3 * 8 + 1

    override fun serialize(item: Trade, data: ByteBuffer) {
        data.putLong(item.time.toEpochMilli())
        data.putDouble(item.price)
        data.putDouble(item.amount)
        data.put(if (item.isBuyerMaker) 1.toByte() else 0.toByte())
    }

    override fun deserialize(data: ByteBuffer): Trade = Trade(
            Instant.ofEpochMilli(data.long),
            data.double,
            data.double,
            data.get() == 1.toByte()
    )
}