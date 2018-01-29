package exchange.history

import data.HistoryCache
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.takeWhile
import util.lang.InstantRange
import util.lang.max
import util.lang.min
import java.time.Duration
import java.time.Instant

class PreloadedMarketHistory(
        private val cache: HistoryCache,
        private val market: String,
        private val original: MarketHistory,
        private val originalPeriod: Duration
) : MarketHistory {
    suspend fun preload(timeRange: InstantRange) {
        val filledRange = cache.filledRange(market)
        if (filledRange != null) {
            if (timeRange.endInclusive >= filledRange.endInclusive.plus(originalPeriod)) {
                fillCandles(filledRange.endInclusive..timeRange.endInclusive, filledRange)
            }
            if (timeRange.start < filledRange.start) {
                fillCandles(timeRange.start..filledRange.start, filledRange)
            }
        } else {
            fillCandles(timeRange, filledRange)
        }
    }

    private suspend fun fillCandles(timeRange: InstantRange, oldAllFilledRange: InstantRange?) {
        if (oldAllFilledRange != null) {
            val fillAfter = timeRange.start == oldAllFilledRange.endInclusive
            val fillBefore = timeRange.endInclusive == oldAllFilledRange.start
            require(fillAfter || fillBefore)
        }

        val candles = original.candlesBefore(timeRange.endInclusive).takeWhile {
            it.timeRange.start >= timeRange.start
        }
        val newAllFilledRange = min(
                timeRange.start,
                oldAllFilledRange?.start ?: Instant.MAX
        )..max(
                timeRange.endInclusive,
                oldAllFilledRange?.endInclusive ?: Instant.MIN
        )
        cache.insertCandles(market, candles, newAllFilledRange)
    }

    override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = cache.candlesBefore(market, time)
}