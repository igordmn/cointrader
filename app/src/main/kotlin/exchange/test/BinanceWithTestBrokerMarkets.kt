package exchange.test

import exchange.LoggableMarketBroker
import exchange.Market
import exchange.Markets
import exchange.SafeMarketBroker
import exchange.binance.BinanceConstants
import exchange.binance.BinanceInfo
import exchange.binance.api.BinanceAPI
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketPrice
import exchange.binance.market.binanceCachePath
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.history.CachedMarketHistory
import exchange.history.NormalizedMarketHistory
import org.mapdb.DBMaker
import util.log.logger
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.Duration

class BinanceWithTestBrokerMarkets(
        private val constants: BinanceConstants,
        private val api: BinanceAPI,
        private val portfolio: TestPortfolio,
        private val fee: BigDecimal,
        private val binanceInfo: BinanceInfo,
        private val operationScale: Int,
        private val period: Duration
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = constants.marketName(fromCoin, toCoin)
        return if (name != null) {
            val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
            val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
            val binanceHistory = CachedMarketHistory(
                    DBMaker.fileDB(binanceCachePath(name).toFile()),
                    BinanceMarketHistory(name, api, logger(BinanceMarketHistory::class)),
                    Duration.ofMinutes(1)
            )
            val history = NormalizedMarketHistory(binanceHistory, normalizer, period)
            val prices = BinanceMarketPrice(name, api)
            val limits = binanceInfo.limits(name)
            val testBroker = TestMarketBroker(fromCoin, toCoin, portfolio, prices, fee, limits, TestMarketBroker.LogListener(logger(TestMarketBroker::class)))
            val safeBroker = SafeMarketBroker(
                    testBroker,
                    limits,
                    attemptCount = 10,
                    attemptAmountDecay = BigDecimal("0.99"),
                    log = logger("" + SafeMarketBroker::class + " $name")
            )
            val broker = LoggableMarketBroker(
                    safeBroker,
                    fromCoin, toCoin,
                    logger(TestMarketBroker::class)
            )
            Market(broker, history, prices)
        } else {
            null
        }
    }
}