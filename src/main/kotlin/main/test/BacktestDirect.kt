package main.test

import adviser.net.NeuralTradeAdviser
import com.binance.api.client.BinanceApiClientFactory
import exchange.Exchange
import exchange.Market
import exchange.Markets
import exchange.binance.*
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketPrice
import exchange.test.TestMarketOrders
import exchange.test.TestPortfolio
import exchange.test.TestTime
import kotlinx.coroutines.experimental.runBlocking
import trader.AdvisableTrade
import trader.TradingBot
import java.math.BigDecimal
import java.nio.file.Paths

fun main(args: Array<String>) {
    val operationScale = 32
    val exchange = testExchange(TestConfig.initialCoins)
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
            exchange.time,
            exchange.markets,
            exchange.portfolio,
            operationScale
    )
    val bot = TradingBot(TestConfig.period, exchange.time, trade)

    runBlocking {
        bot.run()
    }
}

private fun testExchange(initialCoins: Map<String, String>): Exchange {
    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newAsyncRestClient()
    val info = BinanceInfo()
    val portfolio = TestPortfolio(initialCoins.mapValues { BigDecimal(it.value) })
    val markets = object : Markets {
        override fun of(fromCoin: String, toCoin: String): Market? {
            val name = info.marketName(fromCoin, toCoin)
            return if (name != null) {
                val prices = BinanceMarketPrice(name, client)
                val orders = TestMarketOrders(fromCoin, toCoin, portfolio, prices)
                val history = BinanceMarketHistory(name, client)
                Market(orders, history, prices)
            } else {
                null
            }
        }
    }
    val time = TestTime(TestConfig.startTime)
    return Exchange(portfolio, markets, time)
}