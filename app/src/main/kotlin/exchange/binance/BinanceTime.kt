package exchange.binance

import exchange.*
import exchange.binance.api.BinanceAPI
import java.time.Instant

class BinanceTime(
        private val api: BinanceAPI
) : ExchangeTime {
    override suspend fun current(): Instant {
        val serverTime = api.serverTime()
        return Instant.ofEpochMilli(serverTime.serverTime)
    }
}