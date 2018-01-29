package exchange.history

import data.HistoryCache
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.takeWhile
import util.lang.truncatedTo
import java.time.Duration
import java.time.Instant

class PreloadedMarketHistory(
        private val cache: HistoryCache,
        private val market: String,
        private val original: MarketHistory,
        private val originalPeriod: Duration
) : MarketHistory {
    suspend fun preload(endTime: Instant) {
        val lastCloseTime = cache.lastCloseTimeOf(market) ?: Instant.MIN
        if (endTime >= lastCloseTime + originalPeriod) {
            val candles = original.candlesBefore(endTime).takeWhile {
                it.timeRange.start >= lastCloseTime
            }
            cache.insertCandles(market, candles)
        }
    }

    override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = cache.candlesBefore(market, time.truncatedTo(originalPeriod))
}