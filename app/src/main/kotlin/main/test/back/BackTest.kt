package main.test.back

import adviser.net.NeuralTradeAdviser
import com.binance.api.client.domain.general.ExchangeInfo
import exchange.*
import exchange.binance.BinanceInfo
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketLimits
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
    val exchangeInfo = api.exchangeInfo()
    val info = BinanceInfo()
    val portfolio = TestPortfolio(config.initialCoins)
    val time = TestTime(config.startTime)
    val markets = TestMarkets(info, api, time, portfolio, config.fee, exchangeInfo, operationScale)

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
            TradingBot.LogListener(logger(TradingBot::class))
    )

    testTrade.setTimeCloseToNextPeriod()
    bot.run()
}

private class TestMarkets(
        private val info: BinanceInfo,
        private val api: BinanceAPI,
        private val time: ExchangeTime,
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
            val prices = TestHistoricalMarketPrice(time, history, approximatedPricesFactory)
            val limits = BinanceMarketLimits(name, exchangeInfo)
            val testBroker = TestMarketBroker(fromCoin, toCoin, portfolio, prices, fee, limits, TestMarketBroker.LogListener(logger(TestMarketBroker::class)))
            val safeBroker = SafeMarketBroker(testBroker, limits)
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