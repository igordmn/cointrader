package com.dmi.cointrader.train

import com.dmi.cointrader.HistoryPeriods
import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.periods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.neural.jep
import com.dmi.cointrader.neural.networkTrainer
import com.dmi.cointrader.neural.trainingNetwork
import com.dmi.cointrader.trade.performTestTradesAllInFast
import com.dmi.cointrader.trade.performTestTradesPartialFast
import com.dmi.util.collection.contains
import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import kotlinx.coroutines.experimental.channels.take
import java.nio.file.Files.createDirectory
import java.nio.file.Paths

suspend fun trainBatch() = resourceContext {
    val resultsDir = Paths.get("data/resultsBatch")
    resultsDir.deleteRecursively()
    createDirectory(resultsDir)

    val resultsDetailLogFile = resultsDir.resolve("resultsDetail.log")
    val resultsShortLogFile = resultsDir.resolve("resultsShort.log")

    val steps = 40000
    val scoresSkipSteps = 10000
    val repeat = 5
    val percentile = 3.0 / 4

    var num = 0

    with(object{
        suspend fun train(tradeConfig: TradeConfig, trainConfig: TrainConfig, additionalParams: String) {
            num++
            resultsDetailLogFile.appendLine("")
            resultsDetailLogFile.appendLine("    $num")
            resultsDetailLogFile.appendLine(tradeConfig.toString())
            resultsDetailLogFile.appendLine(trainConfig.toString())

            val score = (1..repeat).map { trainSingle(it, tradeConfig, trainConfig, additionalParams) }.sorted()[(repeat * percentile).toInt()]

            resultsShortLogFile.appendLine("")
            resultsShortLogFile.appendLine("    $num")
            resultsShortLogFile.appendLine(tradeConfig.toString())
            resultsShortLogFile.appendLine(trainConfig.toString())
            resultsShortLogFile.appendLine(score.toString())
        }

        suspend fun trainSingle(repeat: Int, tradeConfig: TradeConfig, trainConfig: TrainConfig, additionalParams: String): Double {
            resultsDetailLogFile.appendLine("repeat $repeat")
            resultsShortLogFile.appendLine("repeat $repeat")

            val binanceExchange = binanceExchangeForInfo()
            require(trainConfig.range in tradeConfig.periodSpace.start..binanceExchange.currentTime())
            val periods = trainConfig.range.periods(tradeConfig.periodSpace)
            val archive = archive(
                    tradeConfig.periodSpace, tradeConfig.assets, binanceExchange, periods.last,
                    reloadCount = tradeConfig.archiveReloadPeriods
            )
            val jep = jep()
            val net = trainingNetwork(jep, tradeConfig, additionalParams)
            val trainer = networkTrainer(jep, net, trainConfig.fee, additionalParams)
            val (trainPeriods, testPeriods, validationPeriods) = periods.prepareForTrain(tradeConfig, trainConfig)
            val batches = trainBatches(archive, trainPeriods, tradeConfig, trainConfig)

            var trainProfits = ArrayList<Double>(trainConfig.logSteps)
            val results = ArrayList<TrainResult>()
            batches.channel().take(steps).consumeEachIndexed { (i, it) ->
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
                            )
                    )
                    results.add(result)
                    resultsDetailLogFile.appendLine(result.toString())
                    trainProfits = ArrayList(trainConfig.logSteps)
                }
            }
            fun TrainResult.score() = tests[0].dayProfit
            val scores = results.drop(scoresSkipSteps / trainConfig.logSteps).map { it.score() }.sorted()
            return scores[(scores.size * percentile).toInt()]
        }
    }) {
        train(TradeConfig(), TrainConfig(logSteps = 250), "{learning_rate: 0.00028}")
    }
}