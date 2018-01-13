package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import exchange.Market
import exchange.Markets

class BinanceMarkets(
        private val info: BinanceInfo,
        private val client: BinanceApiAsyncRestClient
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = info.marketName(fromCoin, toCoin)
        return if (name != null) {
            BinanceMarket(
                    name,
                    client,
                    BinanceMarketHistory(name, client)
            )
        } else {
            null
        }
    }
}