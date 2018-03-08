package old.main.test.forward

import adviser.net.neuralTradeAdviser
import old.exchange.binance.BinanceConstants
import old.exchange.binance.BinanceInfo
import old.exchange.binance.BinanceTimeOld
import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.cointrader.app.binance.api.binanceAPI
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
import old.trader.TradingBot
import com.dmi.util.log.logger
import java.time.Instant

fun forwardTest() = runBlocking {
    System.setProperty("log.name", "forwardTest")
    System.setProperty("log.level", "DEBUG")

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

    val operationScale = 32

    val api = binanceAPI(log = LoggerFactory.getLogger(BinanceAPI::class.java))
    val constants = BinanceConstants()
    val portfolio = TestPortfolio(config.initialCoins)
    val time = BinanceTimeOld(api)
    val info = BinanceInfo.load(api)

    makeBinanceCacheDB().use { cache ->
        val preloadedHistories = PreloadedBinanceMarketHistories(cache, constants, api, config.mainCoin, config.altCoins)
        val serverTime = Instant.ofEpochMilli(api.serverTime().serverTime)
        preloadedHistories.preload(serverTime)
        val markets = BinanceWithTestBrokerMarkets(preloadedHistories, constants, api, portfolio, config.fee, info, operationScale, config.period)

        val adviser = neuralTradeAdviser(jep, operationScale, config)
        val trade = AdvisableTrade(
                config.mainCoin,
                config.altCoins,
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