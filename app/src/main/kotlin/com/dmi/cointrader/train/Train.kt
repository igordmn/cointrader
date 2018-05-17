package com.dmi.cointrader.train

import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.periods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.info.saveLogChart
import com.dmi.cointrader.neural.jep
import com.dmi.cointrader.neural.networkTrainer
import com.dmi.cointrader.neural.trainingNetwork
import com.dmi.cointrader.trade.*
import com.dmi.util.collection.contains
import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.dmi.util.io.writeBytes
import com.sun.javafx.application.PlatformImpl
import jep.Jep
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import kotlinx.coroutines.experimental.channels.take
import kotlinx.serialization.cbor.CBOR.Companion.dump
import kotlinx.serialization.list
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

suspend fun train() {
    val tradeConfig = TradeConfig()
    val trainConfig = TrainConfig()
    val jep = jep()
    train(jep, Paths.get("data/results"), tradeConfig, trainConfig, "{}")
}

suspend fun train(jep: Jep, path: Path, tradeConfig: TradeConfig, trainConfig: TrainConfig, additionalParams: String) {
    val binanceExchange = binanceExchangeForInfo()
    require(trainConfig.range in tradeConfig.periodSpace.start..binanceExchange.currentTime())
    val periods = trainConfig.range.periods(tradeConfig.periodSpace)
    val archive = archive(
            tradeConfig.periodSpace, tradeConfig.assets, binanceExchange, periods.last,
            reloadCount = tradeConfig.archiveReloadPeriods
    )
    val (trainPeriods, testPeriods, validationPeriods) = periods.prepareForTrain(tradeConfig, trainConfig)

    PlatformImpl.startup({})

    path.deleteRecursively()

    val scores = ArrayList<Double>()
    repeat(trainConfig.repeats) { repeat ->
        resourceContext {
            val resultsDir = path.resolve("$repeat")
            createDirectories(resultsDir)

            val networksDir = resultsDir.resolve("networks")
            createDirectories(networksDir)
            fun netDir(step: Int) = networksDir.resolve(step.toString())

            val charts1Dir = resultsDir.resolve("charts1")
            val charts2Dir = resultsDir.resolve("charts2")
            createDirectories(charts1Dir)
            createDirectories(charts2Dir)
            fun chart1File(step: Int) = charts1Dir.resolve("$step.png")
            fun chart2File(step: Int) = charts2Dir.resolve("$step.png")

            println("Repeat $repeat")

            val resultsLogFile = resultsDir.resolve("results.log")
            val resultsDumpFile = resultsDir.resolve("results.dump")

            fun log(msg: String) {
                resultsLogFile.appendLine(msg)
                println(msg)
            }

            log(tradeConfig.toString())
            log(trainConfig.toString())
            log(additionalParams)

            var trainProfits = ArrayList<Double>(trainConfig.logSteps)
            val results = ArrayList<TrainResult>()
            val batches = trainBatches(archive, trainPeriods, tradeConfig, trainConfig)
            val net = trainingNetwork(jep, tradeConfig, additionalParams)
            val trainer = networkTrainer(jep, net, trainConfig.fee, additionalParams)

            fun saveNet(result: TrainResult) {
                val netDir = netDir(result.step)
                net.save(netDir)
                netDir.resolve("tradeConfig").writeBytes(dump(tradeConfig))
                saveLogChart(result.tests.last().chartData, chart1File(result.step))
                log(result.toString())
            }

            val channel = batches.channel()
            channel.take(trainConfig.steps).consumeEachIndexed { (i, it) ->
                val (newPortions, trainProfit) = trainer.train(it.currentPortfolio, it.history)
                it.setCurrentPortfolio(newPortions)
                trainProfits.add(trainProfit)
                if ((i + 1) % trainConfig.logSteps == 0) {
                    val result = trainResult(
                            space = tradeConfig.periodSpace,
                            tradePeriods = tradeConfig.tradePeriods.size,
                            step = i + 1,
                            previousResults = results,
                            trainProfits = trainProfits,
                            testCapitals = listOf(
                                    performTestTradesAllInFast(testPeriods, tradeConfig, net, archive, trainConfig.fee)
//                                    performTestTradesAllInFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee)
                            )
                    )
                    results.add(result)
                    saveNet(result)
                    trainProfits = ArrayList(trainConfig.logSteps)
                    if (i >= trainConfig.breakSteps && !results.any { it.tests.last().dayProfit >= trainConfig.breakProfit }) {
                        channel.cancel()
                    }
                }
            }

            val localScores = results.map { it.tests.last().score }.sorted()
            val score = localScores[localScores.size * 3 / 4]
            log("Score $score")
            scores.add(score)

            resultsDumpFile.writeBytes(dump(TrainResult.serializer().list, results))

            if (repeat + 1 >= trainConfig.repeatsBreak && !scores.any { it >= trainConfig.repeatsBreakScore }) {
                return
            }
        }
    }
}