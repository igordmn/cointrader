package train

import adviser.net.DEFAULT_NET_PATH
import adviser.net.NNAgent
import exchange.binance.BinanceConstants
import exchange.binance.api.binanceAPI
import exchange.binance.market.PreloadedBinanceMarketHistories
import exchange.binance.market.makeBinanceCacheDB
import jep.Jep
import kotlinx.coroutines.experimental.runBlocking
import main.test.Config
import python.jep
import java.nio.file.Files

fun main(args: Array<String>) {
    runBlocking {
        DEFAULT_NET_PATH.parent.toFile().deleteRecursively()
        Files.createDirectory(DEFAULT_NET_PATH)

        val config = Config()
        jep().use { jep ->
            agent(jep, config).use { agent ->
                val api = binanceAPI()
                val constants = BinanceConstants()
                makeBinanceCacheDB().use { cache ->
                    val preloadedHistories = PreloadedBinanceMarketHistories(cache, constants, api, config.mainCoin, config.altCoins)
                    preloadedHistories.preload(config.trainEndTime)


                }
            }
        }
    }
}

private fun agent(jep: Jep, config: Config): NNAgent {
    return NNAgent(
            jep,
            config.indicators.count,
            config.altCoins.size + 1,
            config.historyCount,
            config.fee,
            config.learningRate
    )
}