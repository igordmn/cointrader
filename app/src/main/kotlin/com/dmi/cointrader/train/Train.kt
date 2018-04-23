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
import com.dmi.cointrader.saveTradeConfig
import com.dmi.cointrader.trade.performTestTradesAllInFast
import com.dmi.cointrader.trade.performTestTradesPartialFast
import com.dmi.util.collection.contains
import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.sun.javafx.application.PlatformImpl
import jep.Jep
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import kotlinx.coroutines.experimental.channels.take
import java.lang.Math.pow
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log2

typealias TrainScore = Double

suspend fun train() {
    val tradeConfig = TradeConfig()
    val trainConfig = TrainConfig()
    saveTradeConfig(tradeConfig)
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

            val resultsLogFile = resultsDir.resolve("results.log")

            resultsLogFile.appendLine(tradeConfig.toString())
            resultsLogFile.appendLine(trainConfig.toString())
            resultsLogFile.appendLine(additionalParams)

            println("Repeat $repeat")

            var trainProfits = ArrayList<Double>(trainConfig.logSteps)
            val results = ArrayList<TrainResult>()
            val batches = trainBatches(archive, trainPeriods, tradeConfig, trainConfig)
            val net = trainingNetwork(jep, tradeConfig, additionalParams)
            val trainer = networkTrainer(jep, net, trainConfig.fee, additionalParams)

            fun saveNet(result: TrainResult) {
                net.save(netDir(result.step))
                saveLogChart(result.tests[0].chartData, chart1File(result.step))
//                saveLogChart(result.tests[1].chartData, chart2File(result.step))
                resultsLogFile.appendLine(result.toString())
                println(result.toString())
            }

            val channel = batches.channel()
            channel.take(trainConfig.steps).consumeEachIndexed { (i, it) ->
                val (newPortions, trainProfit) = trainer.train(it.currentPortfolio, it.history)
                it.setCurrentPortfolio(newPortions)
                trainProfits.add(trainProfit)
                if (i % trainConfig.logSteps == 0) {
                    val result = trainResult(
                            space = tradeConfig.periodSpace,
                            tradePeriods = tradeConfig.tradePeriods.size,
                            step = i,
                            previousResults = results,
                            trainProfits = trainProfits,
                            testCapitals = listOf(
                                    performTestTradesAllInFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee)
//                                    performTestTradesPartialFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee)
//                                    performTestTradesPartialFast(testPeriods, tradeConfig, net, archive, trainConfig.fee)
//                                    performTestTradesPartialFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee)
                            )
                    )
                    results.add(result)
                    saveNet(result)
                    trainProfits = ArrayList(trainConfig.logSteps)

                    if (i >= trainConfig.breakSteps && !results.any { it.tests[0].dayProfitMean >= trainConfig.breakProfit }) {
                        channel.cancel()
                    }
                }
            }

            fun TrainResult.score() = tests[0].dayProfitMean
            val scores = results.drop(trainConfig.scoresSkipSteps / trainConfig.logSteps).map { it.score() }.sorted()
            val score = scores[scores.size * 3 / 4]
            resultsLogFile.appendLine("Score $score")
            println("Score $score")
        }
    }
}