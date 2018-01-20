package exchange.binance

import exchange.Market
import exchange.Markets
import exchange.binance.api.BinanceAPI
import exchange.binance.market.BinanceMarketBroker
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketPrice
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import util.log.logger

class BinanceMarkets(
        private val info: BinanceInfo,
        private val api: BinanceAPI,
        private val operationScale: Int
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = info.marketName(fromCoin, toCoin)
        return if (name != null) {
            val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
            val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
            val history = BinanceMarketHistory(name, api, normalizer)
            val prices = BinanceMarketPrice(name, api)
            val broker = BinanceMarketBroker(name, api, logger(BinanceMarketBroker::class))
            Market(broker, history, prices)
        } else {
            null
        }
    }
}