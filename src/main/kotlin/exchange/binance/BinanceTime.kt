package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import exchange.*
import java.time.Instant
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceTime(
        private val client: BinanceApiAsyncRestClient
) : ExchangeTime {
    override suspend fun current(): Instant = suspendCoroutine { continuation ->
        client.getServerTime {
            continuation.resume(Instant.ofEpochMilli(it.serverTime))
        }
    }
}