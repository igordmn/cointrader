package exchange

import java.time.Instant

interface ExchangeTime {
    suspend fun current(): Instant
}