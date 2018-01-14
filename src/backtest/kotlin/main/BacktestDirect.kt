package main

import adviser.net.NeuralTradeAdviser
import com.binance.api.client.BinanceApiClientFactory
import exchange.Exchange
import exchange.Market
import exchange.Markets
import exchange.binance.*
import exchange.test.TestMarket
import exchange.test.TestPortfolio
import kotlinx.coroutines.experimental.launch
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
            exchange,
            operationScale
    )
    val bot = TradingBot(TestConfig.period, exchange, trade)

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
                val prices = BinancePrices(name, client)
                val history = BinanceMarketHistory(name, client)
                TestMarket(fromCoin, toCoin, portfolio, prices, history)
            } else {
                null
            }
        }
    }
    return BinanceExchange(portfolio, markets, client)
}