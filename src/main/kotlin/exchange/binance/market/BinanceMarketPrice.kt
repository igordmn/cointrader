package exchange.binance.market

import com.binance.api.client.BinanceApiAsyncRestClient
import exchange.MarketPrice
import exchange.binance.api.BinanceAPI
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceMarketPrice(
        private val name: String,
        private val api: BinanceAPI
): MarketPrice {
    override suspend fun current(): BigDecimal {
        val result = api.latestPrices.await()
        val ticket = result.find { it.symbol == name }!!
        return BigDecimal(ticket.price)
    }
}