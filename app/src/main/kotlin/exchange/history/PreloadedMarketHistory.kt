package exchange.history

import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.*
import org.mapdb.*
import org.mapdb.serializer.GroupSerializerObjectArray
import org.mapdb.serializer.SerializerBigDecimal
import util.ext.mapdb.InstantSerializer
import java.time.Duration
import java.time.Instant


class PreloadedMarketHistory(
        private val db: DB,
        private val original: MarketHistory,
        private val originalPeriod: Duration
) : MarketHistory {
    suspend fun preloadBefore(time: Instant) {
        map(db).use { map ->
            val lastCloseTime = map.lastKey2() ?: Instant.MIN
            if (time >= lastCloseTime.plus(originalPeriod)) {
                original.candlesBefore(time).takeWhile {
                    it.timeRange.start >= lastCloseTime
                }.consumeEach {
                            map[it.timeRange.endInclusive] = it
                        }
                db.commit()
            }
        }
    }

    override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
        map(db).use { map ->
            map.headMap(time, true)
                    .descendingMap()
                    .values
                    .forEach {
                        send(it)
                    }
        }
    }

    private fun map(it: DB) = it.treeMap(
            "map",
            InstantSerializer,
            TimedCandleSerializer
    ).createOrOpen()

    private object TimedCandleSerializer : GroupSerializerObjectArray<TimedCandle>() {
        private val instantSerializer = InstantSerializer
        private val bigDecimalSerializer = SerializerBigDecimal()

        override fun serialize(out: DataOutput2, value: TimedCandle) {
            instantSerializer.serialize(out, value.timeRange.start)
            instantSerializer.serialize(out, value.timeRange.endInclusive)
            bigDecimalSerializer.serialize(out, value.item.open)
            bigDecimalSerializer.serialize(out, value.item.close)
            bigDecimalSerializer.serialize(out, value.item.high)
            bigDecimalSerializer.serialize(out, value.item.low)
        }

        override fun deserialize(input: DataInput2, available: Int): TimedCandle {
            val start = instantSerializer.deserialize(input, available)
            val endInclusive = instantSerializer.deserialize(input, available)
            val open = bigDecimalSerializer.deserialize(input, available)
            val close = bigDecimalSerializer.deserialize(input, available)
            val high = bigDecimalSerializer.deserialize(input, available)
            val low = bigDecimalSerializer.deserialize(input, available)
            return TimedCandle(start..endInclusive, Candle(open, close, high, low))
        }
    }
}