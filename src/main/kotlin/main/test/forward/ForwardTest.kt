package main.test.forward

import adviser.net.NeuralTradeAdviser
import com.binance.api.client.domain.general.ExchangeInfo
import exchange.LoggableMarketBroker
import exchange.Market
import exchange.Markets
import exchange.binance.BinanceInfo
import exchange.binance.BinanceTime
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketLimits
import exchange.binance.market.BinanceMarketPrice
import exchange.test.TestMarketBroker
import exchange.test.TestPortfolio
import kotlinx.coroutines.experimental.runBlocking
import main.test.TestConfig
import org.slf4j.LoggerFactory
import trader.AdvisableTrade
import trader.TradingBot
import util.log.logger
import java.math.BigDecimal
import java.nio.file.Paths

fun main(args: Array<String>) = runBlocking {
    System.setProperty("org.slf4j.simpleLogger.logFile", Paths.get("forwardTest.log").toString())
    val log = LoggerFactory.getLogger("main")

    log.info("Config:\n$TestConfig")

    try {
        run()
    } catch (e: Exception) {
        log.error("Error on running", e)
    }
}

private suspend fun run() {
    val operationScale = 32

    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo.await()
    val info = BinanceInfo()
    val portfolio = TestPortfolio(TestConfig.initialCoins.mapValues { BigDecimal(it.value) })
    val time = BinanceTime(api)
    val markets = TestMarkets(info, api, portfolio, TestConfig.fee, exchangeInfo)

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

    val bot = TradingBot(
            TestConfig.period, time, trade,
            TradingBot.LogListener(logger(TradingBot::class))
    )

    bot.run()
}

private class TestMarkets(
        private val info: BinanceInfo,
        private val api: BinanceAPI,
        private val portfolio: TestPortfolio,
        private val fee: BigDecimal,
        private val exchangeInfo: ExchangeInfo
) : Markets {
    override fun of(fromCoin: String, toCoin: String): Market? {
        val name = info.marketName(fromCoin, toCoin)
        return if (name != null) {
            val history = BinanceMarketHistory(name, api)
            val prices = BinanceMarketPrice(name, api)
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