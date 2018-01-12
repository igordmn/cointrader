package exchange

import java.time.Instant

interface Exchange {
    fun portfolio(): Portfolio
    fun marketFor(fromCoin: String, toCoin: String): Market
    suspend fun serverTime(): Instant
}