package com.dmi.cointrader.data

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Duration
import java.time.Instant

@Entity
data class TradeMomentsMeta(
        @Id var id: Long = 0,

        @Convert(converter = InstantConverter::class, dbType = Long::class)
        val startTime: Instant,

        @Convert(converter = InstantConverter::class, dbType = Long::class)
        val endTime: Instant,

        @Convert(converter = DurationConverter::class, dbType = Long::class)
        val period: Duration,

        @Convert(converter = StringListConverter::class, dbType = ByteArray::class)
        val coins: List<String>
) {
    class StringListConverter : PropertyConverter<List<String>, ByteArray> {
        override fun convertToEntityProperty(data: ByteArray): List<String> {
            val stream = ObjectInputStream(ByteArrayInputStream(data))
            val size = stream.readInt()
            val strings = ArrayList<String>(size)
            for (i in 0 until size) {
                strings.add(stream.readUTF())
            }
            return strings
        }

        override fun convertToDatabaseValue(strings: List<String>): ByteArray {
            val bs = ByteArrayOutputStream()
            ObjectOutputStream(bs).use { os ->
                os.writeInt(strings.size)
                strings.forEach {
                    os.writeUTF(it)
                }
            }
            return bs.toByteArray()
        }
    }

    class InstantConverter : PropertyConverter<Instant, Long> {
        override fun convertToEntityProperty(data: Long): Instant = Instant.ofEpochMilli(data)
        override fun convertToDatabaseValue(instant: Instant): Long = instant.toEpochMilli()
    }

    class DurationConverter : PropertyConverter<Duration, Long> {
        override fun convertToEntityProperty(data: Long): Duration = Duration.ofMillis(data)
        override fun convertToDatabaseValue(duration: Duration): Long = duration.toMillis()
    }
}