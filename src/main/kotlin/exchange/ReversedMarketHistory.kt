package exchange

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

class ReversedMarketHistory(
        private val original: MarketHistory,
        private val operationScale: Int
): MarketHistory {
    override suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> {
        fun Candle.reverse() = Candle(
                close = BigDecimal.ONE.divide(close, operationScale, RoundingMode.HALF_UP),
                open = BigDecimal.ONE.divide(open, operationScale, RoundingMode.HALF_UP),
                low = BigDecimal.ONE.divide(high, operationScale, RoundingMode.HALF_UP),
                high = BigDecimal.ONE.divide(low, operationScale, RoundingMode.HALF_UP)
        )

        return original.candlesBefore(time, count, period).map(Candle::reverse)
    }
}