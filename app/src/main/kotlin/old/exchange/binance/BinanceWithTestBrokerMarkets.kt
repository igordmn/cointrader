package old.exchange.binance

import old.exchange.LoggableMarketBroker
import old.exchange.OldMarket
import old.exchange.Markets
import old.exchange.SafeMarketBroker
import com.dmi.cointrader.app.binance.api.BinanceAPI
import old.exchange.binance.market.BinanceMarketBroker
import old.exchange.binance.market.BinanceMarketPrice
import old.exchange.binance.market.PreloadedBinanceMarketHistories
import old.exchange.candle.LinearApproximatedPricesFactory
import old.exchange.candle.approximateCandleNormalizer
import old.exchange.history.NormalizedMarketHistory
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
    override fun of(fromCoin: String, toCoin: String): OldMarket? {
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
            OldMarket(broker, history, prices)
        } else {
            null
        }
    }
}