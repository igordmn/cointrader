package exchange

import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class ReversedMarketHistory(
        private val original: MarketHistory,
        private val operationScale: Int
): MarketHistory {
    override suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> {
        fun Candle.reverse() = Candle(
                closePrice = BigDecimal.ONE.divide(closePrice, operationScale),
                openPrice = BigDecimal.ONE.divide(openPrice, operationScale),
                lowPrice = BigDecimal.ONE.divide(highPrice, operationScale),
                highPrice = BigDecimal.ONE.divide(lowPrice, operationScale)
        )

        return original.candlesBefore(time, count, period).map(Candle::reverse)
    }
}