package main.test.back

import adviser.net.NeuralTradeAdviser
import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain.general.ExchangeInfo
import exchange.*
import exchange.binance.*
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketLimits
import exchange.binance.market.loadExchangeInfo
import exchange.test.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import main.test.TestConfig
import trader.AdvisableTrade
import trader.Trade
import trader.TradingBot
import util.lang.truncatedTo
import util.log.logger
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

fun main(args: Array<String>) = runBlocking {
    System.setProperty("org.slf4j.simpleLogger.logFile", Paths.get("backTest.log").toString())
    val log = Logger.getLogger("main")

    log.info("Config:\n$TestConfig")

    val operationScale = 32

    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newAsyncRestClient()
    val exchangeInfo = loadExchangeInfo(client)
    val info = BinanceInfo()
    val portfolio = TestPortfolio(TestConfig.initialCoins.mapValues { BigDecimal(it.value) })
    val time = TestTime(TestConfig.startTime)
    val markets = TestMarkets(info, client, time, portfolio, TestConfig.fee, exchangeInfo)

    val adviser = NeuralTradeAdviser(
            TestConfig.mainCoin,
            TestConfig.altCoins,
            TestConfig.historyCount,
            Paths.get("D:/Development/Projects/coin_predict/train_package2/netfile"),
            TestConfig.fee,
            TestConfig.indicators
    )
    val trade = AdvisableTrade(
            TestConfig.mainCoin,
            TestConfig.altCoins,
            TestConfig.period,
            TestConfig.historyCount,
            adviser,
            markets,
            portfolio,
            operationScale,
            AdvisableTrade.LogListener(logger(AdvisableTrade::class))
    )

    val testTrade = TestTrade(trade, time, TestConfig.period)
    val bot = TradingBot(
            TestConfig.period, time, trade,
            TradingBot.LogListener(logger(TradingBot::class))
    )

    testTrade.setTimeCloseToNextPeriod()
    bot.run()
}

private class TestMarkets(
        private val info: BinanceInfo,
        private val client: BinanceApiAsyncRestClient,
        private val time: ExchangeTime,
        private val portfolio: TestPortfolio,
        private val fee: BigDecimal,
        private val exchangeInfo: ExchangeInfo
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = info.marketName(fromCoin, toCoin)
        return if (name != null) {
            val history = BinanceMarketHistory(name, client)
            val prices = TestHistoricalMarketPrice(time, history)
            val limits = BinanceMarketLimits(name, exchangeInfo)
            val orders = TestMarketBroker(fromCoin, toCoin, portfolio, prices, fee, limits)
            Market(orders, history, prices)
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
        val distance = Duration.ofMillis(100)
        val currentTime = time.current()
        val nextPeriodTime = currentTime.truncatedTo(period) + period
        time.setCurrent(nextPeriodTime - distance)
    }
}