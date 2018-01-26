package exchange.history

import exchange.candle.CandleNormalizer
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import java.time.Duration
import java.time.Instant

class NormalizedMarketHistory(
        private val original: MarketHistory,
        private val normalizer: CandleNormalizer,
        private val period: Duration
) : MarketHistory {
    override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> {
        val candles = original.candlesBefore(time)
        return normalizer.normalizeBefore(candles, time, period)
    }
}