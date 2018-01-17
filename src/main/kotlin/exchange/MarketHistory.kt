package exchange

import kotlinx.coroutines.experimental.channels.produce
import java.time.Duration
import java.time.Instant

interface MarketHistory {
    suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle>
}