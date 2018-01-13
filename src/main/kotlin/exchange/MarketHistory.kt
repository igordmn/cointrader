package exchange

import java.time.Duration
import java.time.Instant

interface MarketHistory {
    suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle>
}