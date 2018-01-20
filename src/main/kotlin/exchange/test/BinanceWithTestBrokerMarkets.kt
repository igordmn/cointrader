package exchange.test

import com.binance.api.client.domain.general.ExchangeInfo
import exchange.LoggableMarketBroker
import exchange.Market
import exchange.Markets
import exchange.binance.BinanceInfo
import exchange.binance.api.BinanceAPI
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketLimits
import exchange.binance.market.BinanceMarketPrice
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.test.TestMarketBroker
import exchange.test.TestPortfolio
import util.log.logger
import java.math.BigDecimal

class BinanceWithTestBrokerMarkets(
        private val info: BinanceInfo,
        private val api: BinanceAPI,
        private val portfolio: TestPortfolio,
        private val fee: BigDecimal,
        private val exchangeInfo: ExchangeInfo,
        private val operationScale: Int
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = info.marketName(fromCoin, toCoin)
        return if (name != null) {
            val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
            val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
            val history = BinanceMarketHistory(name, api, normalizer)
            val prices = BinanceMarketPrice(name, api)
            val limits = BinanceMarketLimits(name, exchangeInfo)
            val broker = LoggableMarketBroker(
                    TestMarketBroker(fromCoin, toCoin, portfolio, prices, fee, limits, TestMarketBroker.LogListener(logger(TestMarketBroker::class))),
                    fromCoin, toCoin,
                    logger(TestMarketBroker::class)
            )
            Market(broker, history, prices)
        } else {
            null
        }
    }
}