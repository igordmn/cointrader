package main.test.forward

import adviser.net.NeuralTradeAdviser
import exchange.binance.BinanceConstants
import exchange.binance.BinanceInfo
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
import trader.TradingBot
import util.log.logger
import util.python.PythonUtils
import java.nio.file.Paths

fun forwardTest() = runBlocking {
    System.setProperty("log.name", "forwardTest")

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
    val constants = BinanceConstants()
    val portfolio = TestPortfolio(config.initialCoins)
    val time = BinanceTime(api)
    val info = BinanceInfo.load(api)
    val markets = BinanceWithTestBrokerMarkets(constants, api, portfolio, config.fee, info, operationScale)

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

    val bot = TradingBot(
            config.period, time, trade,
            TradingBot.LogListener(logger(TradingBot::class)),
            {
                info.refresh()
            }
    )

    bot.run()
}