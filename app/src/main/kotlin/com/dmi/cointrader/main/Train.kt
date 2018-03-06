package com.dmi.cointrader.main

import com.dmi.cointrader.app.candle.candleNum
import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.NeuralTrainer
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.concurrent.mapAsync
import com.dmi.util.io.SyncList
import com.dmi.util.math.rangeSample
import exchange.binance.BinanceConstants
import exchange.binance.api.binanceAPI
import jep.Jep
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.runBlocking
import main.test.Config
import org.apache.commons.math3.distribution.GeometricDistribution
import python.jep
import java.nio.file.Files
import java.nio.file.Paths

private val netsPath = Paths.get("data/nets")

fun main(args: Array<String>) {
    runBlocking {
        netsPath.toFile().deleteRecursively()
        Files.createDirectory(netsPath)

        val config = Config()

        val api = binanceAPI()
        val constants = BinanceConstants()
        val currentTime = MemoryAtom(config.trainEndTime)

        fun coinLog(coin: String) = object : SyncList.Log<Trade> {
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
        val endPeriodNum = candleNum(config.startTime, config.period, config.trainEndTime).coerceAtMost(moments.size())

        jep().use { jep ->
            network(jep, config).use { net ->
                trainer(jep, config, net).use { trainer ->
//                    train(trainer, config, moments, startPeriodNum until endPeriodNum)
                }
            }
        }
    }
}

private fun train(trainer: NeuralTrainer, config: Config, moments: SyncList<Moment>, nums: LongRange): Any {
    val random = GeometricDistribution(config.trainGeometricBias)
    val firstNum = nums.first.coerceAtLeast(config.historyCount + config.trainBatchSize - 1L).toInt()
    val lastNum = nums.last.toInt()
    val batchLastNum = random.rangeSample(firstNum..lastNum)
    val batchFirstNum = batchLastNum - config.trainBatchSize + 1

}

private suspend fun batch(
        moments: SyncList<Moment>,
        config: Config,
        lastIndex: Long
): TrainBatch {

}

private fun network(jep: Jep, config: Config) = NeuralNetwork.init(
        jep,
        NeuralNetwork.Config(config.altCoins.size, config.historyCount, 3),
        gpuMemoryFraction = 0.5
)

private fun trainer(jep: Jep, config: Config, net: NeuralNetwork) = NeuralTrainer(
        jep,
        net,
        NeuralTrainer.Config(config.fee.toDouble())
)

private class TrainBatch(val moments: List<TrainMoment>)

private class TrainMoment(val history: List<Moment>, val portfolio: List<Double>, val futurePriceIncs: List<Double>) {
    suspend fun setPortfolio(portofolio: List<Double>) {
        TODO()
    }
}