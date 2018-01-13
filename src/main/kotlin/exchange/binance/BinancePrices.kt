package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import exchange.MarketHistory
import exchange.Prices
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class BinancePrices(
        private val name: String,
        private val client: BinanceApiAsyncRestClient
): Prices {
    override suspend fun current(): BigDecimal = suspendCoroutine { continuation ->
        client.getAllPrices { allPrices ->
            val ticket = allPrices.find { it.symbol == name }!!
            val price = BigDecimal(ticket.price)
            continuation.resume(price)
        }
    }
}