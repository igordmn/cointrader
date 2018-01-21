package main.test.back

import adviser.net.NeuralTradeAdviser
import exchange.*
import exchange.binance.BinanceConstants
import exchange.binance.BinanceInfo
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketHistory
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.test.TestHistoricalMarketPrice
import exchange.test.TestMarketBroker
import exchange.test.TestPortfolio
import exchange.test.TestTime
import kotlinx.coroutines.experimental.delay
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
import java.util.concurrent.TimeUnit

fun backTest() = runBlocking {
    System.setProperty("log.name", "backTest")

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
    val time = TestTime(config.startTime)
    val info = BinanceInfo.load(api)
    val markets = TestMarkets(constants, api, time, portfolio, config.fee, info, operationScale)

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
            config.period,
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
            {
                info.refresh()
            }
    )

    testTrade.setTimeCloseToNextPeriod()
    bot.run()
}

private class TestMarkets(
        private val constants: BinanceConstants,
        private val api: BinanceAPI,
        private val time: ExchangeTime,
        private val portfolio: TestPortfolio,
        private val fee: BigDecimal,
        private val binanceInfo: BinanceInfo,
        private val operationScale: Int
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = constants.marketName(fromCoin, toCoin)
        return if (name != null) {
            val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
            val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
            val history = BinanceMarketHistory(name, api, normalizer)
            val prices = TestHistoricalMarketPrice(time, history, approximatedPricesFactory)
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
    // todo при ошибке, будет ddosить сервер каждые 100мс
    private val distanceBeforePeriodStart = Duration.ofMillis(100)

    override suspend fun perform(time: Instant) {
        val periodStart = (this.time.current + distanceBeforePeriodStart).truncatedTo(period)
        require(periodStart == this.time.current + distanceBeforePeriodStart)
        this.time.current = periodStart + Duration.ofMillis(50)
        delay(50, TimeUnit.MILLISECONDS)
        original.perform(time)
        setTimeCloseToNextPeriod()
    }

    suspend fun setTimeCloseToNextPeriod() {
        val currentTime = time.current()
        val nextPeriodTime = currentTime.truncatedTo(period) + period
        time.current = nextPeriodTime - distanceBeforePeriodStart
    }
}