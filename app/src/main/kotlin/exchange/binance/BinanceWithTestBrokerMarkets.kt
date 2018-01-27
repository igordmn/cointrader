package exchange.binance

import exchange.LoggableMarketBroker
import exchange.Market
import exchange.Markets
import exchange.SafeMarketBroker
import exchange.binance.api.BinanceAPI
import exchange.binance.market.*
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.history.PreloadedMarketHistory
import exchange.history.NormalizedMarketHistory
import org.mapdb.DBMaker
import util.log.logger
import java.math.BigDecimal
import java.time.Duration

class BinanceMarkets(
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
            val binanceHistory = preloadedBinanceMarketHistory(api, name)
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