package main.test.forward

import adviser.net.NeuralTradeAdviser
import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.BinanceApiClientFactory
import exchange.Market
import exchange.Markets
import exchange.binance.BinanceInfo
import exchange.binance.BinanceTime
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketPrice
import exchange.test.TestMarketBroker
import exchange.test.TestPortfolio
import kotlinx.coroutines.experimental.runBlocking
import main.test.TestConfig
import trader.AdvisableTrade
import trader.TradingBot
import java.math.BigDecimal
import java.nio.file.Paths

fun main(args: Array<String>) {
    val operationScale = 32

    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newAsyncRestClient()
    val info = BinanceInfo()
    val portfolio = TestPortfolio(TestConfig.initialCoins.mapValues { BigDecimal(it.value) })
    val time = BinanceTime(client)
    val markets = TestMarkets(info, client, portfolio, TestConfig.fee)

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

    val bot = TradingBot(TestConfig.period, time, trade)

    runBlocking {
        bot.run()
    }
}

private class TestMarkets(
        private val info: BinanceInfo,
        private val client: BinanceApiAsyncRestClient,
        private val portfolio: TestPortfolio,
        private val fee: BigDecimal
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = info.marketName(fromCoin, toCoin)
        return if (name != null) {
            val history = BinanceMarketHistory(name, client)
            val prices = BinanceMarketPrice(name, client)
            val orders = TestMarketBroker(fromCoin, toCoin, portfolio, prices, fee)
            Market(orders, history, prices)
        } else {
            null
        }
    }
}