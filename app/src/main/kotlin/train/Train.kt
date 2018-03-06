package train

import adviser.net.DEFAULT_NET_PATH
import adviser.net.NNAgent
import com.dmi.cointrader.app.candle.candleNum
import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.atom.synchronizable
import com.dmi.util.concurrent.mapAsync
import com.dmi.util.io.SyncList
import exchange.binance.BinanceConstants
import exchange.binance.BinanceTime
import exchange.binance.api.binanceAPI
import exchange.binance.market.PreloadedBinanceMarketHistories
import exchange.binance.market.makeBinanceCacheDB
import jep.Jep
import kotlinx.coroutines.experimental.runBlocking
import main.test.Config
import python.jep
import java.nio.file.Files
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    runBlocking {
        DEFAULT_NET_PATH.parent.toFile().deleteRecursively()
        Files.createDirectory(DEFAULT_NET_PATH)

        val config = Config()

        val api = binanceAPI()
        val constants = BinanceConstants()
        val currentTime = MemoryAtom(config.trainEndTime)

        fun coinLog(coin: String) = object: SyncList.Log<Trade> {
            override fun itemsAppended(items: List<Trade>, indices: LongRange) {
                val lastTradeTime = items.last().time
                println("$coin $lastTradeTime")
            }
        }

        val coinToTrades = coinToCachedBinanceTrades(config, constants, api, currentTime, ::coinLog)
        val moments = cachedMoments(config, coinToTrades, currentTime)

        println("Download trades")
        coinToTrades.mapAsync {
            it.sync()
        }

        println("Make moments")
        moments.sync()

        val startPeriodNum = candleNum(config.startTime, config.period, config.trainStartTime)
        val endPeriodNum = candleNum(config.startTime, config.period, config.trainEndTime)

        jep().use { jep ->
            agent(jep, config).use { agent ->

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