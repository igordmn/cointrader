package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import exchange.*
import exchange.binance.api.BinanceAPI
import java.time.Instant
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceTime(
        private val api: BinanceAPI
) : ExchangeTime {
    override suspend fun current(): Instant {
        val serverTime = api.serverTime()
        return Instant.ofEpochMilli(serverTime.serverTime)
    }
}