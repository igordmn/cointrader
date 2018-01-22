package exchange.binance.market

import exchange.MarketPrice
import exchange.binance.api.BinanceAPI
import java.math.BigDecimal

class BinanceMarketPrice(
        private val name: String,
        private val api: BinanceAPI
): MarketPrice {
    override suspend fun current(): BigDecimal {
        val result = api.allPrices()
        val ticket = result.find { it.symbol == name }!!
        return BigDecimal(ticket.price)
    }
}