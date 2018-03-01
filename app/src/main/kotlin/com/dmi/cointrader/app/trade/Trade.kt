package com.dmi.cointrader.app.trade

import com.dmi.util.collection.Indexed
import com.dmi.util.io.FixedSerializer
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.time.Instant

@Serializable
data class Trade(val time: Instant, val amount: Double, val price: Double) {
    fun reverse() = Trade(time, amount * price, 1 / price)
}

typealias IndexedTrade<INDEX> = Indexed<INDEX, Trade>

class TradeFixedSerializer : FixedSerializer<Trade> {
    override val itemBytes: Int = 3 * 8

    override fun serialize(item: Trade, data: ByteBuffer) {
        data.putLong(item.time.toEpochMilli())
        data.putDouble(item.amount)
        data.putDouble(item.price)
    }

    override fun deserialize(data: ByteBuffer): Trade = Trade(
            Instant.ofEpochMilli(data.long),
            data.double,
            data.double
    )
}