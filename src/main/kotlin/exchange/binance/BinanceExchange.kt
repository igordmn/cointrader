package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import exchange.Exchange
import exchange.Market
import exchange.Markets
import exchange.Portfolio
import java.time.Instant
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceExchange(
        override val portfolio: Portfolio,
        override val markets: Markets,
        private val client: BinanceApiAsyncRestClient
) : Exchange {
    override suspend fun currentTime(): Instant = suspendCoroutine { continuation ->
        client.getServerTime {
            continuation.resume(Instant.ofEpochSecond(it.serverTime))
        }
    }
}