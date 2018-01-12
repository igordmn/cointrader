package exchange

import java.time.Duration
import java.time.Instant

typealias CoinToCandles = Map<String, List<Candle>>

interface MarketHistory {
    suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle>

}