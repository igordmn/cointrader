package main.real

import adviser.net.NeuralTradeAdviser
import exchange.binance.BinanceInfo
import exchange.binance.BinanceMarkets
import exchange.binance.BinancePortfolio
import exchange.binance.BinanceTime
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.test.BinanceWithTestBrokerMarkets
import exchange.test.TestPortfolio
import kotlinx.coroutines.experimental.runBlocking
import main.test.TestConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import trader.AdvisableTrade
import trader.MultipleTrade
import trader.TradingBot
import util.log.logger
import util.python.PythonUtils
import java.nio.file.Paths

fun main(args: Array<String>) = runBlocking {
    println("Run trading real money? enter 'yes' if yes")
    val answer = readLine()
    if (answer != "yes") {
        println("Answer not 'yes', so exit")
        return@runBlocking
    }

    System.setProperty("log.name", "realTest")

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
    val testPortfolio = TestPortfolio(config.initialCoins)
    val testPortfolio2 = TestPortfolio(config.initialCoins)
    val binancePortfolio = BinancePortfolio(info, api)
    val time = BinanceTime(api)
    val testMarkets = BinanceWithTestBrokerMarkets(info, api, testPortfolio, config.fee, exchangeInfo, operationScale)
    val testMarkets2 = BinanceWithTestBrokerMarkets(info, api, testPortfolio2, config.fee, exchangeInfo, operationScale)
    val binanceMarkets = BinanceMarkets(info, api, operationScale)

    val adviser = NeuralTradeAdviser(
            config.mainCoin,
            config.altCoins,
            config.historyCount,
            Paths.get("train_package/netfile"),
            config.fee,
            config.indicators
    )
    val testTrade = AdvisableTrade(
            config.mainCoin,
            config.altCoins,
            config.period,
            config.historyCount,
            adviser,
            testMarkets,
            testPortfolio,
            operationScale,
            AdvisableTrade.LogListener(logger(AdvisableTrade::class.qualifiedName + " test"))
    )
    val testTrade2 = AdvisableTrade(
            config.mainCoin,
            config.altCoins,
            config.period,
            config.historyCount,
            adviser,
            testMarkets2,
            testPortfolio2,
            operationScale,
            AdvisableTrade.LogListener(logger(AdvisableTrade::class.qualifiedName + " test"))
    )
    val binanceTrade = AdvisableTrade(
            config.mainCoin,
            config.altCoins,
            config.period,
            config.historyCount,
            adviser,
            binanceMarkets,
            binancePortfolio,
            operationScale,
            AdvisableTrade.LogListener(logger(AdvisableTrade::class.qualifiedName + " real"))
    )
    val trade = MultipleTrade(listOf(testTrade, testTrade2))

    val bot = TradingBot(
            config.period, time, trade,
            TradingBot.LogListener(logger(TradingBot::class))
    )

    bot.run()
}