package exchange.history

import exchange.candle.TimedCandle
import exchange.candle.timedCandleSerializer
import kotlinx.coroutines.experimental.channels.*
import org.mapdb.BTreeMap
import org.mapdb.DB
import org.mapdb.DBMaker
import util.ext.mapdb.InstantSerializer
import util.ext.mapdb.kotlinSerializer
import java.time.Duration
import java.time.Instant


class CachedMarketHistory(
        private val dbMaker: DBMaker.Maker,
        private val original: MarketHistory,
        private val originalPeriod: Duration
): MarketHistory {
    override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
        dbMaker.make().use {
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

    private suspend fun fillBefore(map: BTreeMap<Instant, TimedCandle>, time: Instant) {
        val lastCloseTime = map.lastKey2() ?: Instant.MIN
        if (time >= lastCloseTime.plus(originalPeriod)) {
            original.candlesBefore(time).takeWhile {
                it.timeRange.start >= lastCloseTime
            }.consumeEach {
                map.put(it.timeRange.endInclusive, it)
            }
        }
    }

    private fun map(it: DB) = it.treeMap(
            "map",
            InstantSerializer,
            kotlinSerializer<TimedCandle>(timedCandleSerializer)
    ).createOrOpen()
}