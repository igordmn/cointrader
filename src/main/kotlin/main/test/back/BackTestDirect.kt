package main.test.back

import adviser.net.NeuralTradeAdviser
import com.binance.api.client.domain.general.ExchangeInfo
import exchange.ExchangeTime
import exchange.LoggableMarketBroker
import exchange.Market
import exchange.Markets
import exchange.binance.BinanceInfo
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketLimits
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

fun main(args: Array<String>) = runBlocking {
    System.setProperty("log.name", "backTest")

    val log = LoggerFactory.getLogger("main")

    try {
        PythonUtils.startPython()
        run(log)
    } catch (e: Exception) {
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
    val exchangeInfo = api.exchangeInfo.await()
    val info = BinanceInfo()
    val portfolio = TestPortfolio(config.initialCoins)
    val time = TestTime(config.startTime)
    val markets = TestMarkets(info, api, time, portfolio, config.fee, exchangeInfo, operationScale)

    val adviser = NeuralTradeAdviser(
            config.mainCoin,
            config.altCoins,
            config.historyCount,
            Paths.get("D:/Development/Projects/coin_predict/train_package2/netfile"),
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
            config.period, time, trade,
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
            val history = BinanceMarketHistory(name, api)
            val prices = TestHistoricalMarketPrice(time, history, operationScale)
            val limits = BinanceMarketLimits(name, exchangeInfo)
            val broker = LoggableMarketBroker(
                    TestMarketBroker(fromCoin, toCoin, portfolio, prices, fee, limits),
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
    override suspend fun perform(time: Instant) {
        delay(50, TimeUnit.MILLISECONDS)
        original.perform(time)
        setTimeCloseToNextPeriod()
    }

    suspend fun setTimeCloseToNextPeriod() {
        val distance = Duration.ofMillis(3000)
        val currentTime = time.current()
        val nextPeriodTime = currentTime.truncatedTo(period) + period
        time.current = nextPeriodTime - distance
    }
}