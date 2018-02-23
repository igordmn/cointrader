package com.dmi.cointrader.data

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.converter.PropertyConverter
import io.objectbox.relation.ToOne
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Instant

@Entity
data class TradeChunk(
        @Id var id: Long = 0,
        @Index val market: ToOne<Market>,
        val lastTradeId: Long,

        @Convert(converter = TradeListConverter::class, dbType = ByteArray::class)
        val list: List<Trade>
) {
    data class Trade(val time: Instant, val amount: Double, val price: Double)

    class TradeListConverter : PropertyConverter<List<Trade>, ByteArray> {
        private val itemByteCount = 8 + 8 + 8  // Long, Double, Double

        override fun convertToEntityProperty(data: ByteArray): List<Trade> {
            val size = data.size / itemByteCount
            val stream = ObjectInputStream(ByteArrayInputStream(data))
            val trades = ArrayList<Trade>(size)
            for (i in 0 until size) {
                trades.add(Trade(
                        time = Instant.ofEpochMilli(stream.readLong()),
                        amount = stream.readDouble(),
                        price = stream.readDouble()
                ))
            }
            return trades
        }

        override fun convertToDatabaseValue(trades: List<Trade>): ByteArray {
            val bs = ByteArrayOutputStream()
            ObjectOutputStream(bs).use { os ->
                trades.forEach {
                    os.writeLong(it.time.toEpochMilli())
                    os.writeDouble(it.amount)
                    os.writeDouble(it.price)
                }
            }
            return bs.toByteArray()
        }
    }
}