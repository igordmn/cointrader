package old.main.test.back

import adviser.net.neuralTradeAdviser
import old.exchange.*
import old.exchange.binance.BinanceConstants
import old.exchange.binance.BinanceInfo
import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.cointrader.app.binance.api.binanceAPI
import old.exchange.binance.market.PreloadedBinanceMarketHistories
import old.exchange.binance.market.makeBinanceCacheDB
import old.exchange.candle.LinearApproximatedPricesFactory
import old.exchange.candle.approximateCandleNormalizer
import old.exchange.history.NormalizedMarketHistory
import old.exchange.test.TestHistoricalMarketPrice
import old.exchange.test.TestMarketBroker
import old.exchange.test.TestPortfolio
import old.exchange.test.TestTime
import kotlinx.coroutines.experimental.runBlocking
import com.dmi.cointrader.main.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.dmi.cointrader.app.python.jep
import old.trader.AdvisableTrade
import old.trader.Trade
import old.trader.TradingBot
import com.dmi.util.lang.truncatedTo
import com.dmi.util.log.logger
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

fun backTest() = runBlocking {
    System.setProperty("log.name", "backTest")
    System.setProperty("log.level", "DEBUG")

    val log = LoggerFactory.getLogger("old/main")

    try {
        run(log)
    } catch (e: Throwable) {
        log.error("Error on running", e)
    }
}

private suspend fun run(log: Logger) = jep().use { jep ->
    val config = Config()
    log.info("Config:\n$config")

    val operationScale = 32

    val api = binanceAPI(log = LoggerFactory.getLogger(BinanceAPI::class.java))
    val constants = BinanceConstants()
    val portfolio = TestPortfolio(config.initialCoins)
    val time = TestTime(config.backTestStartTime)
    val info = BinanceInfo.load(api)

    makeBinanceCacheDB().use { cache ->
        val preloadedHistories = PreloadedBinanceMarketHistories(cache, constants, api, config.mainCoin, config.altCoins)
        val serverTime = Instant.ofEpochMilli(api.serverTime().serverTime)
        preloadedHistories.preload(serverTime)
        val markets = TestMarkets(preloadedHistories, constants, time, portfolio, config.fee, info, operationScale, config.period)

        val adviser = neuralTradeAdviser(jep, operationScale, config)
        val trade = AdvisableTrade(
                config.mainCoin,
                config.altCoins,
                config.historyCount,
                adviser,
                markets,
                portfolio,
                operationScale,
                AdvisableTrade.LogListener(logger(AdvisableTrade::class))
        )

        val testTrade = TestTrade(trade, time, config.period)
        val bot = TradingBot(
                config.period, time, testTrade,
                TradingBot.LogListener(logger(TradingBot::class)),
                { time ->
                    preloadedHistories.preload(time)
                },
                {}
        )

        testTrade.setTimeCloseToNextPeriod()
        bot.run()
    }
}

private class TestMarkets(
        private val preloadedBinanceMarketHistories: PreloadedBinanceMarketHistories,
        private val constants: BinanceConstants,
        private val time: ExchangeTime,
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
            val oneMinuteHistory = NormalizedMarketHistory(binanceHistory, normalizer, Duration.ofMinutes(1))
            val prices = TestHistoricalMarketPrice(time, oneMinuteHistory, approximatedPricesFactory)
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

private class TestTrade(
        private val original: Trade,
        private val time: TestTime,
        private val period: Duration
) : Trade {
    private val distanceBeforePeriodStart = Duration.ofMillis(10)

    override suspend fun perform(time: Instant) {
        val periodStart = (this.time.current + distanceBeforePeriodStart).truncatedTo(period)
        require(periodStart == this.time.current + distanceBeforePeriodStart)
        this.time.current = periodStart + Duration.ofMillis(10)
        original.perform(time)
        setTimeCloseToNextPeriod()
    }

    suspend fun setTimeCloseToNextPeriod() {
        val currentTime = time.current()
        val nextPeriodTime = currentTime.truncatedTo(period) + period
        time.current = nextPeriodTime - distanceBeforePeriodStart
    }
}