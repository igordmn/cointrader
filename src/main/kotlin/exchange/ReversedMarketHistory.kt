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
                closePrice = BigDecimal.ONE.divide(closePrice, operationScale, RoundingMode.HALF_UP),
                openPrice = BigDecimal.ONE.divide(openPrice, operationScale, RoundingMode.HALF_UP),
                lowPrice = BigDecimal.ONE.divide(highPrice, operationScale, RoundingMode.HALF_UP),
                highPrice = BigDecimal.ONE.divide(lowPrice, operationScale, RoundingMode.HALF_UP)
        )

        return original.candlesBefore(time, count, period).map(Candle::reverse)
    }
}