package exchange

import exchange.candle.Candle
import java.time.Duration
import java.time.Instant

interface MarketHistory {
    suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle>
}