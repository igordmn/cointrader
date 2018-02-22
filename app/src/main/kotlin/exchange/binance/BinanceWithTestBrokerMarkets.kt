package exchange.binance

import exchange.LoggableMarketBroker
import exchange.Market
import exchange.Markets
import exchange.SafeMarketBroker
import exchange.binance.api.BinanceAPI
import exchange.binance.market.BinanceMarketBroker
import exchange.binance.market.BinanceMarketPrice
import exchange.binance.market.PreloadedBinanceMarketHistories
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.history.NormalizedMarketHistory
import com.dmi.util.log.logger
import java.math.BigDecimal
import java.time.Duration

class BinanceMarkets(
        private val preloadedBinanceMarketHistories: PreloadedBinanceMarketHistories,
        private val constants: BinanceConstants,
        private val api: BinanceAPI,
        private val binanceInfo: BinanceInfo,
        private val operationScale: Int,
        private val period: Duration
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = constants.marketName(fromCoin, toCoin)
        return if (name != null) {
            val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
            val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
            val binanceHistory = preloadedBinanceMarketHistories[name]
            val history = NormalizedMarketHistory(binanceHistory, normalizer, period)
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