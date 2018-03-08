package old.main.real

import adviser.net.neuralTradeAdviser
import old.exchange.binance.*
import old.exchange.binance.api.BinanceAPI
import old.exchange.binance.api.binanceAPI
import old.exchange.binance.market.PreloadedBinanceMarketHistories
import old.exchange.binance.market.makeBinanceCacheDB
import old.exchange.test.BinanceWithTestBrokerMarkets
import old.exchange.test.TestPortfolio
import kotlinx.coroutines.experimental.runBlocking
import com.dmi.cointrader.main.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.dmi.cointrader.app.python.jep
import old.trader.AdvisableTrade
import old.trader.MultipleTrade
import old.trader.TradingBot
import com.dmi.util.log.logger
import java.io.File
import java.time.Instant

fun realTrade() = runBlocking {
    println("Run trading real money? enter 'yes' if yes")
    val answer = readLine()
    if (answer != "yes") {
        println("Answer not 'yes', so exit")
        return@runBlocking
    }

    System.setProperty("log.name", "realTrade")
    System.setProperty("log.level", "TRACE")

    val log = LoggerFactory.getLogger("old/main")

    try {
        run(log)
    } catch (e: Throwable) {
        log.error("Error on running", e)
    }
}

private suspend fun run(log: Logger) = jep().use { jep ->
    val config = Config()
    log.info("Config:\n$config")

    val apiKey = File("E:/Distr/Data/CryptoExchanges/binance/apiKey.txt").readText()
    val secret = File("E:/Distr/Data/CryptoExchanges/binance/secret.txt").readText()

    val operationScale = 32

    val api = binanceAPI(apiKey, secret, LoggerFactory.getLogger(BinanceAPI::class.java), 10)
    val constants = BinanceConstants()
    val binancePortfolio = BinancePortfolio(constants, api)
    val testPortfolio = TestPortfolio(config.initialCoins)
    val testPortfolio2 = TestPortfolio(config.initialCoins)
    val time = BinanceTimeOld(api)
    val info = BinanceInfo.load(api)

    makeBinanceCacheDB().use { cache ->
        val preloadedHistories = PreloadedBinanceMarketHistories(cache, constants, api, config.mainCoin, config.altCoins)
        val serverTime = Instant.ofEpochMilli(api.serverTime().serverTime)
        preloadedHistories.preload(serverTime)

        val binanceMarkets = BinanceMarkets(preloadedHistories, constants, api, info, operationScale, config.period)
        val testMarkets = BinanceWithTestBrokerMarkets(preloadedHistories, constants, api, testPortfolio, config.fee, info, operationScale, config.period)
        val testMarkets2 = BinanceWithTestBrokerMarkets(preloadedHistories, constants, api, testPortfolio2, config.fee, info, operationScale, config.period)

        val adviser = neuralTradeAdviser(jep, operationScale, config)
        val binanceTrade = AdvisableTrade(
                config.mainCoin,
                config.altCoins,
                config.historyCount,
                adviser,
                binanceMarkets,
                binancePortfolio,
                operationScale,
                AdvisableTrade.LogListener(logger(AdvisableTrade::class.qualifiedName + " real"))
        )
        val testTrade = AdvisableTrade(
                config.mainCoin,
                config.altCoins,
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
                config.historyCount,
                adviser,
                testMarkets2,
                testPortfolio2,
                operationScale,
                AdvisableTrade.LogListener(logger(AdvisableTrade::class.qualifiedName + " test"))
        )
//    val trade = MultipleTrade(listOf(testTrade, testTrade2))
        val trade = MultipleTrade(listOf(binanceTrade, testTrade))

        val bot = TradingBot(
                config.period, time, trade,
                TradingBot.LogListener(logger(TradingBot::class)),
                { time ->
                    preloadedHistories.preload(time)
                },
                {
                    info.refresh()
                }
        )

        bot.run()
    }
}