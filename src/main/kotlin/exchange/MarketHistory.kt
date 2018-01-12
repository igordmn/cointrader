package exchange

import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

typealias CoinToCandles = Map<String, List<MarketHistory.Candle>>

interface MarketHistory {
    suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle>

    data class Candle(
            val openPrice: BigDecimal,
            val closePrice: BigDecimal,
            val highPrice: BigDecimal,
            val lowPrice: BigDecimal
    )
}