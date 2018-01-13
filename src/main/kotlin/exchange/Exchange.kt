package exchange

import java.time.Instant

interface Exchange {
    val portfolio: Portfolio
    val markets: Markets
    suspend fun currentTime(): Instant
}