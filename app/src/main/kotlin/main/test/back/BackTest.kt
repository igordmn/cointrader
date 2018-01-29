package main.test.back

import adviser.net.NeuralTradeAdviser
import exchange.*
import exchange.binance.BinanceConstants
import exchange.binance.BinanceInfo
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.PreloadedBinanceMarketHistories
import exchange.binance.market.makeBinanceCacheDB
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.history.NormalizedMarketHistory
import exchange.test.TestHistoricalMarketPrice
import exchange.test.TestMarketBroker
import exchange.test.TestPortfolio
import exchange.test.TestTime
import kotlinx.coroutines.experimental.runBlocking
import main.test.TestConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import trader.AdvisableTrade
import trader.Trade
import trader.TradingBot
import util.lang.truncatedTo
import util.log.logger
import util.python.PythonUtils
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

fun backTest() = runBlocking {
    System.setProperty("log.name", "backTest")
    System.setProperty("log.level", "DEBUG")

    val log = LoggerFactory.getLogger("main")

    try {
        PythonUtils.startPython()
        run(log)
    } catch (e: Throwable) {
        log.error("Error on running", e)
    } finally {
        PythonUtils.stopPython()
    }
}

private suspend fun run(log: Logger) {
    val config = TestConfig()
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
        preloadedHistories.preload(config.preloadStartTime, serverTime)
        val markets = TestMarkets(preloadedHistories, constants, time, portfolio, config.fee, info, operationScale, config.period)

        val adviser = NeuralTradeAdviser(
                config.mainCoin,
                config.altCoins,
                config.historyCount,
                Paths.get("data/train_package/netfile"),
                config.fee,
                config.indicators
        )
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
                    preloadedHistories.preload(config.preloadStartTime, time)
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