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
import com.dmi.cointrader.trade.TradeSummary
import com.dmi.cointrader.trade.performTestTradesAllInFast
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
import org.apache.commons.io.FileUtils
import java.lang.Math.pow
import java.nio.file.Files
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.math.sqrt

typealias TrainScore = Double

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

    val bestPath = path.resolve("best")
    bestPath.deleteRecursively()

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
                saveLogChart(result.tests[0].chartData, chart1File(result.step))
//                saveLogChart(result.tests[1].chartData, chart2File(result.step))
                log(result.toString())
            }

            fun saveBestNet(result: TrainResult) {
                FileUtils.copyDirectory(netDir(result.step).toFile(), bestPath.resolve("$repeat").toFile())
                Files.copy(chart1File(result.step), bestPath.resolve("$repeat.png"))
                bestPath.resolve("results.log").appendLine(result.toString())
            }

            fun bestResult(results: List<TrainResult>): TrainResult {
                val tradeSummaryNeighborMean = IdentityHashMap<TradeSummary, Double>()

                fun TradeSummary.neighborMean() = tradeSummaryNeighborMean[this] ?: 0.0

                for (i in 2 until results.size - 2) {
                    val r1 = results[i - 2]
                    val r2 = results[i - 1]
                    val r3 = results[i]
                    val r4 = results[i + 1]
                    val r5 = results[i + 2]
                    r3.tests.forEachIndexed { ti, it ->
                        tradeSummaryNeighborMean[it] = pow(
                                r1.tests[ti].dayProfitMean *
                                        r2.tests[ti].dayProfitMean *
                                        r3.tests[ti].dayProfitMean *
                                        r4.tests[ti].dayProfitMean *
                                        r5.tests[ti].dayProfitMean, 0.2)
                    }
                }

                val selectors = listOf(
                        { it: TrainResult -> it.tests[1].neighborMean() },
                        { it: TrainResult -> it.tests[1].dayProfitMean },
                        { it: TrainResult -> it.tests[1].dayProfitMedian },
                        { it: TrainResult -> it.tests[1].downsideDeviation },
                        { it: TrainResult -> it.tests[1].maximumDrawdawn },
                        { it: TrainResult -> it.tests[0].neighborMean() },
                        { it: TrainResult -> it.tests[0].dayProfitMean },
                        { it: TrainResult -> it.tests[0].dayProfitMedian }
//                        { it: TrainResult -> it.tests[1].downsideDeviation },
//                        { it: TrainResult -> it.tests[1].maximumDrawdawn },
//                        { it: TrainResult -> -it.tests[0].dailyDownsideDeviation },
//                        { it: TrainResult -> -it.tests[0].dailyMaximumDrawdawn }
                )

                val linkedResults = LinkedList(results)
                var selInd = 0
                while (linkedResults.size > 1) {
                    val selector = selectors[selInd]
                    val min = linkedResults.minBy(selector)
                    linkedResults.remove(min)
                    selInd = (selInd + 1) % selectors.size
                }

                return linkedResults.first
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
//                                    performTestTradesPartialFast(testPeriods, tradeConfig, net, archive, trainConfig.fee),
//                                    performTestTradesPartialFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee),
                                    performTestTradesAllInFast(testPeriods, tradeConfig, net, archive, trainConfig.fee),
                                    performTestTradesAllInFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee)
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
            val localScores = results.map { it.score() }.sorted()
            val score = localScores[localScores.size * 3 / 4]
            log("Score $score")
            scores.add(score)

            val bestResult = bestResult(results)
            saveBestNet(bestResult)

            if (repeat + 1 >= trainConfig.repeatsBreak && !scores.any { it >= trainConfig.repeatsBreakScore }) {
                return
            }
        }
    }
}