package com.dmi.cointrader.train

import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TradePeriods
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.periods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.neural.jep
import com.dmi.cointrader.neural.networkTrainer
import com.dmi.cointrader.neural.trainingNetwork
import com.dmi.cointrader.trade.performTestTradesAllInFast
import com.dmi.util.collection.contains
import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.parseInstantRange
import com.dmi.util.lang.zoneOffset
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import kotlinx.coroutines.experimental.channels.take
import java.nio.file.Files.createDirectories
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

suspend fun trainBatch() {
    val jep = jep()

    val resultsDir = Paths.get("data/resultsBatch")
    resultsDir.deleteRecursively()
    createDirectories(resultsDir)

    val resultsDetailLogFile = resultsDir.resolve("resultsDetail.log")
    val resultsShortLogFile = resultsDir.resolve("resultsShort.log")

    val scoresSkipSteps = 10000
    val breakSteps = 20000
    val breakProfit = 1.06

    fun trainConfig() = TrainConfig(
            range = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parseInstantRange("2017-07-01T00:00:00", "2018-04-20T00:45:00", zoneOffset("+3")),
            validationDays = 20.0,
            steps = 40000,
            repeats = 3,
            logSteps = 1000
    )

    var num = 0

    with(object {
        suspend fun train(tradeConfig: TradeConfig, trainConfig: TrainConfig, additionalParams: String) {
            num++
            resultsDetailLogFile.appendLine("")
            resultsDetailLogFile.appendLine("    $num")
            resultsDetailLogFile.appendLine(tradeConfig.toString())
            resultsDetailLogFile.appendLine(trainConfig.toString())
            resultsDetailLogFile.appendLine(additionalParams)

            val score = try {
                (1..trainConfig.repeats).map {
                    resultsDetailLogFile.appendLine("repeat $it")
                    val score = trainSingle(tradeConfig, trainConfig, additionalParams)
                    resultsDetailLogFile.appendLine("score $score")
                    score
                }.max()
            } catch (e: Exception) {
                e.printStackTrace()
                0.0
            }

            resultsShortLogFile.appendLine("")
            resultsShortLogFile.appendLine("    $num")
            resultsShortLogFile.appendLine(tradeConfig.toString())
            resultsShortLogFile.appendLine(trainConfig.toString())
            resultsShortLogFile.appendLine(additionalParams)
            resultsShortLogFile.appendLine(score.toString())
        }

        suspend fun trainSingle(tradeConfig: TradeConfig, trainConfig: TrainConfig, additionalParams: String): Double = resourceContext {
            val binanceExchange = binanceExchangeForInfo()
            require(trainConfig.range in tradeConfig.periodSpace.start..binanceExchange.currentTime())
            val periods = trainConfig.range.periods(tradeConfig.periodSpace)
            val archive = archive(
                    tradeConfig.periodSpace, tradeConfig.assets, binanceExchange, periods.last,
                    reloadCount = tradeConfig.archiveReloadPeriods
            )
            val net = trainingNetwork(jep, tradeConfig, additionalParams)
            val trainer = networkTrainer(jep, net, trainConfig.fee, additionalParams)
            val (trainPeriods, testPeriods, validationPeriods) = periods.prepareForTrain(tradeConfig, trainConfig)
            val batches = trainBatches(archive, trainPeriods, tradeConfig, trainConfig)
            var trainProfits = ArrayList<Double>(trainConfig.logSteps)
            val results = ArrayList<TrainResult>()
            val channel = batches.channel().take(trainConfig.steps)
            channel.consumeEachIndexed { (i, it) ->
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
                    println(result.toString())
                    resultsDetailLogFile.appendLine(result.toString())
                    trainProfits = ArrayList(trainConfig.logSteps)

                    if (i >= breakSteps && !results.any { it.tests[0].dayProfitMean >= breakProfit }) {
                        channel.cancel()
                    }
                }
            }
            fun TrainResult.score() = tests[0].dayProfitMean
            val scores = results.drop(scoresSkipSteps / trainConfig.logSteps).map { it.score() }.sorted()
            scores[scores.size * 3 / 4]
        }
    }) {
        fun historyPeriods(count: Int) = TradeConfig().historyPeriods.copy(count = count)
        fun historyPeriods2(minutes: Int) = TradeConfig().historyPeriods.copy(size = minutes * TradeConfig().periodSpace.periodsPerMinute().toInt())
        fun tradePeriods(minutes: Int): TradePeriods = TradePeriods(size = minutes * TradeConfig().periodSpace.periodsPerMinute().toInt(), delay = 1)

        train(TradeConfig(), trainConfig(), "{}")
    }
}