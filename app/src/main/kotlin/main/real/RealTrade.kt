package main.real

import adviser.net.NeuralTradeAdviser
import exchange.binance.*
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
import java.io.File
import java.nio.file.Paths

fun realTrade() = runBlocking {
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

    val apiKey = File("E:/Distr/Data/CryptoExchanges/binance/apiKey.txt").readText()
    val secret = File("E:/Distr/Data/CryptoExchanges/binance/secret.txt").readText()

    val operationScale = 32

    val api = binanceAPI(log = LoggerFactory.getLogger(BinanceAPI::class.java))
//    val api = binanceAPI(apiKey, secret, LoggerFactory.getLogger(BinanceAPI::class.java))
    val constants = BinanceConstants()
    val testPortfolio = TestPortfolio(config.initialCoins)
    val testPortfolio2 = TestPortfolio(config.initialCoins)
    val binancePortfolio = BinancePortfolio(constants, api)
    val time = BinanceTime(api)
    val info = BinanceInfo.load(api)
    val testMarkets = BinanceWithTestBrokerMarkets(constants, api, testPortfolio, config.fee, info, operationScale)
    val testMarkets2 = BinanceWithTestBrokerMarkets(constants, api, testPortfolio2, config.fee, info, operationScale)
    val binanceMarkets = BinanceMarkets(constants, api, info, operationScale)

    val adviser = NeuralTradeAdviser(
            config.mainCoin,
            config.altCoins,
            config.historyCount,
            Paths.get("data/train_package/netfile"),
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