package old.exchange.test

import old.exchange.LoggableMarketBroker
import old.exchange.Market
import old.exchange.Markets
import old.exchange.SafeMarketBroker
import old.exchange.binance.BinanceConstants
import old.exchange.binance.BinanceInfo
import old.exchange.binance.api.BinanceAPI
import old.exchange.binance.market.BinanceMarketPrice
import old.exchange.binance.market.PreloadedBinanceMarketHistories
import old.exchange.candle.LinearApproximatedPricesFactory
import old.exchange.candle.approximateCandleNormalizer
import old.exchange.history.NormalizedMarketHistory
import com.dmi.util.log.logger
import java.math.BigDecimal
import java.time.Duration

class BinanceWithTestBrokerMarkets(
        private val preloadedBinanceMarketHistories: PreloadedBinanceMarketHistories,
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
            val binanceHistory = preloadedBinanceMarketHistories[name]
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