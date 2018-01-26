package exchange.history

import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.*
import org.mapdb.BTreeMap
import org.mapdb.DB
import org.mapdb.DBMaker
import util.ext.mapdb.InstantSerializer
import util.ext.mapdb.SerializableSerializer
import java.nio.file.Path
import java.time.Duration
import java.time.Instant


class HistoryCache(
        private val path: Path,
        private val loadCandlesByMinuteBefore: (time: Instant) -> ReceiveChannel<TimedCandle>
) {
    fun candlesByMinuteBefore(time: Instant): ReceiveChannel<TimedCandle> {
        return produce {
            db().use {
                val map = map(it)
                fillBefore(map, time)
                map.headMap(time, true)
                        .descendingMap()
                        .values
                        .forEach {
                            send(it)
                        }
            }
        }
    }

    private suspend fun fillBefore(map: BTreeMap<Instant, TimedCandle>, time: Instant) {
        val lastCloseTime = map.lastKey2() ?: Instant.MIN
        if (time >= lastCloseTime.plus(Duration.ofMinutes(1))) {
            loadCandlesByMinuteBefore(time).takeWhile {
                it.timeRange.start >= lastCloseTime
            }.consumeEach {
                map.put(it.timeRange.endInclusive, it)
            }
        }
    }

    private fun db() = DBMaker.fileDB(path.toFile()).make()
    private fun map(it: DB) = it.treeMap("map", InstantSerializer, SerializableSerializer<TimedCandle>()).createOrOpen()
}