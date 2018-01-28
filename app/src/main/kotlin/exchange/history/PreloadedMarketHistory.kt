package exchange.history

import data.HistoryCache
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.takeWhile
import java.time.Duration
import java.time.Instant

class PreloadedMarketHistory(
        private val cache: HistoryCache,
        private val market: String,
        private val original: MarketHistory,
        private val originalPeriod: Duration
) : MarketHistory {
    suspend fun preload(startTime: Instant, endTime: Instant) {
        val previousStartTime = cache.startTimeOf(market)
        val previousEndTime = cache.endTimeOf(market)
        if (endTime >= previousEndTime.plus(originalPeriod)) {
            val candles = original.candlesBefore(endTime).takeWhile {
                it.timeRange.start >= previousEndTime
            }
            cache.insertCandles(market, candles, previousStartTime, endTime)
        }
        if (startTime < previousStartTime) {
            val candles = original.candlesBefore(previousStartTime).takeWhile {
                it.timeRange.start >= startTime
            }
            cache.insertCandles(market, candles, startTime, endTime)
        }
    }

    override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = cache.candlesBefore(market, time)
}