package exchange.binance

import exchange.LoggableMarketBroker
import exchange.Market
import exchange.Markets
import exchange.SafeMarketBroker
import exchange.binance.api.BinanceAPI
import exchange.binance.market.BinanceMarketBroker
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketPrice
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import util.log.logger
import java.math.BigDecimal

class BinanceMarkets(
        private val constants: BinanceConstants,
        private val api: BinanceAPI,
        private val binanceInfo: BinanceInfo,
        private val operationScale: Int
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = constants.marketName(fromCoin, toCoin)
        return if (name != null) {
            val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
            val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
            val history = BinanceMarketHistory(name, api, normalizer)
            val prices = BinanceMarketPrice(name, api)
            val limits = binanceInfo.limits(name)
            val binanceBroker = BinanceMarketBroker(name, api, logger(BinanceMarketBroker::class))
            val safeBroker = SafeMarketBroker(
                    binanceBroker,
                    limits,
                    attemptCount = 10,
                    attemptAmountDecay = BigDecimal("0.99"),
                    log = logger("" + SafeMarketBroker::class + " $name")
            )
            val broker = LoggableMarketBroker(
                    safeBroker,
                    fromCoin, toCoin,
                    logger(BinanceMarketBroker::class)
            )
            Market(broker, history, prices)
        } else {
            null
        }
    }
}