package exchange.history

import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.*
import org.mapdb.*
import org.mapdb.serializer.GroupSerializerObjectArray
import org.mapdb.serializer.SerializerBigDecimal
import util.ext.mapdb.InstantSerializer
import java.nio.file.Path
import java.time.Duration
import java.time.Instant


class PreloadedMarketHistory(
        private val path: Path,
        private val table: String,
        private val original: MarketHistory,
        private val originalPeriod: Duration
) : MarketHistory {
    suspend fun preloadBefore(time: Instant) {
        db().use { db->
            db.historyMap().use { map ->
                val lastCloseTime = map.lastKey2() ?: Instant.MIN
                if (time >= lastCloseTime.plus(originalPeriod)) {
                    db.transaction {
                        original.candlesBefore(time)
                                .takeWhile {
                                    it.timeRange.start >= lastCloseTime
                                }.consumeEach {
                                    map[it.timeRange.endInclusive] = it
                                }
                    }
                }
            }
        }
    }

    override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
        db().use { db ->
            db.historyMap().use { map ->
                map.headMap(time, true)
                        .descendingMap()
                        .values
                        .forEach {
                            send(it)
                        }
            }
        }
    }

    private fun DB.historyMap(): BTreeMap<Instant, TimedCandle> {
        return treeMap(
                table,
                InstantSerializer,
                TimedCandleSerializer
        ).createOrOpen()
    }

    private suspend fun DB.transaction(action: suspend () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            rollback()
            throw e
        }
        commit()
    }

    private fun db() = DBMaker.fileDB(path.toFile()).transactionEnable().make()

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