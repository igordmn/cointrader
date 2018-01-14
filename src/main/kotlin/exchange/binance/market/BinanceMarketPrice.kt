package exchange.binance.market

import com.binance.api.client.BinanceApiAsyncRestClient
import exchange.MarketPrice
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceMarketPrice(
        private val name: String,
        private val client: BinanceApiAsyncRestClient
): MarketPrice {
    override suspend fun current(): BigDecimal = suspendCoroutine { continuation ->
        client.getAllPrices { allPrices ->
            val ticket = allPrices.find { it.symbol == name }!!
            val price = BigDecimal(ticket.price)
            continuation.resume(price)
        }
    }
}