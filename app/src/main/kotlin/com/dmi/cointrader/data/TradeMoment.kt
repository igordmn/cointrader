package com.dmi.cointrader.data

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.converter.PropertyConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@Entity
data class TradeMoment(
        @Id var id: Long = 0,

        @Index
        val num: Long = 0,

        @Convert(converter = CandleListConverter::class, dbType = ByteArray::class)
        val candles: List<Candle> = emptyList()
) {
    data class Candle(val close: Double, val high: Double, val low: Double)

    class CandleListConverter : PropertyConverter<List<Candle>, ByteArray> {
        private val itemByteCount = 8 + 8 + 8  // Double, Double, Double

        override fun convertToEntityProperty(data: ByteArray?): List<Candle>? {
            if (data == null)
                return null

            val size = data.size / itemByteCount
            val stream = ObjectInputStream(ByteArrayInputStream(data))
            val candles = ArrayList<Candle>(size)
            for (i in 0 until size) {
                candles.add(Candle(
                        close = stream.readDouble(),
                        high = stream.readDouble(),
                        low = stream.readDouble()
                ))
            }
            return candles
        }

        override fun convertToDatabaseValue(trades: List<Candle>?): ByteArray? {
            if (trades == null)
                return null

            val bs = ByteArrayOutputStream()
            ObjectOutputStream(bs).use { os ->
                trades.forEach {
                    os.writeDouble(it.close)
                    os.writeDouble(it.high)
                    os.writeDouble(it.low)
                }
            }
            return bs.toByteArray()
        }
    }
}