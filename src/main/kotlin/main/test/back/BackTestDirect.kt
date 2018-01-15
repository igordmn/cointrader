package main.test.back

import adviser.net.NeuralTradeAdviser
import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.BinanceApiClientFactory
import exchange.*
import exchange.binance.*
import exchange.binance.market.BinanceMarketHistory
import exchange.test.TestMarketBroker
import exchange.test.TestMarketPrice
import exchange.test.TestPortfolio
import exchange.test.TestTime
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import main.test.TestConfig
import trader.AdvisableTrade
import trader.Trade
import trader.TradingBot
import util.lang.truncatedTo
import java.math.BigDecimal
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val operationScale = 32

    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newAsyncRestClient()
    val info = BinanceInfo()
    val portfolio = TestPortfolio(TestConfig.initialCoins.mapValues { BigDecimal(it.value) })
    val time = TestTime(TestConfig.startTime)
    val markets = TestMarkets(info, client, time, portfolio, TestConfig.fee)

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
            time,
            adviser,
            markets,
            portfolio,
            operationScale
    )

    val testTrade = TestTrade(trade, time, TestConfig.period)
    val bot = TradingBot(TestConfig.period, time, trade)

    runBlocking {
        testTrade.setTimeCloseToNextPeriod()
        bot.run()
    }
}

private class TestMarkets(
        private val info: BinanceInfo,
        private val client: BinanceApiAsyncRestClient,
        private val time: ExchangeTime,
        private val portfolio: TestPortfolio,
        private val fee: BigDecimal
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = info.marketName(fromCoin, toCoin)
        return if (name != null) {
            val history = BinanceMarketHistory(name, client)
            val prices = TestMarketPrice(time, history)
            val orders = TestMarketBroker(fromCoin, toCoin, portfolio, prices, fee)
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
    override suspend fun perform() {
        delay(50, TimeUnit.MILLISECONDS)
        original.perform()
        setTimeCloseToNextPeriod()
    }

    suspend fun setTimeCloseToNextPeriod() {
        val distance = Duration.ofMillis(100)
        val currentTime = time.current()
        val nextPeriodTime = currentTime.truncatedTo(period) + period
        time.setCurrent(nextPeriodTime - distance)
    }
}