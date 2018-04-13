package com.dmi.cointrader.train

import com.dmi.cointrader.HistoryPeriods
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
import com.dmi.cointrader.trade.performTestTradesPartialFast
import com.dmi.util.collection.contains
import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import kotlinx.coroutines.experimental.channels.take
import java.nio.file.Files.createDirectory
import java.nio.file.Paths

suspend fun trainBatch() {
    val jep = jep()

    val resultsDir = Paths.get("data/resultsBatch")
    resultsDir.deleteRecursively()
    createDirectory(resultsDir)

    val resultsDetailLogFile = resultsDir.resolve("resultsDetail.log")
    val resultsShortLogFile = resultsDir.resolve("resultsShort.log")

    val steps = 40000
    val scoresSkipSteps = 10000
    val breakSteps = 20000
    val breakProfit = 1.06
    val repeats = 3

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
                (1..repeats).map {
                    resultsDetailLogFile.appendLine("repeat $it")
                    trainSingle(it, tradeConfig, trainConfig, additionalParams)
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

        suspend fun trainSingle(repeat: Int, tradeConfig: TradeConfig, trainConfig: TrainConfig, additionalParams: String): Double = resourceContext {
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
            val channel = batches.channel().take(steps)
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

                    if (i >= breakSteps && !results.any { it.tests[0].dayProfit >= breakProfit }) {
                        channel.cancel()
                    }
                }
            }
            fun TrainResult.score() = tests[0].dayProfit
            val scores = results.drop(scoresSkipSteps / trainConfig.logSteps).map { it.score() }.sorted()
            scores[scores.size * 3 / 4]
        }
    }) {
        fun historyPeriods(count: Int) = TradeConfig().historyPeriods.copy(count = count)
        fun historyPeriods2(minutes: Int) = TradeConfig().historyPeriods.copy(size = minutes * TradeConfig().periodSpace.periodsPerMinute().toInt())
        fun tradePeriods(minutes: Int): TradePeriods = TradePeriods(size = minutes * TradeConfig().periodSpace.periodsPerMinute().toInt(), delay = 1)

        train(TradeConfig(), TrainConfig(), "{'init':'variance_scaling'}")
        train(TradeConfig(), TrainConfig(), "{'init':'normal'}")
//        train(TradeConfig(), TrainConfig(), "{'activation':'prelu'}")
//        train(TradeConfig(), TrainConfig(), "{'activation':'elu'}")
//        train(TradeConfig(), TrainConfig(), "{'activation':'leaky_relu'}")
//        train(TradeConfig(), TrainConfig(), "{'activation':'selu'}")
//        train(TradeConfig(), TrainConfig(), "{'activation':'crelu'}")
//        train(TradeConfig(), TrainConfig(), "{'activation':'relu6'}")
//        train(TradeConfig(), TrainConfig(), "{'kernel_size':7}")
//        train(TradeConfig(), TrainConfig(), "{'kernel_size':3}")
//        train(TradeConfig(), TrainConfig(), "{'kernel_size':2}")
//        train(TradeConfig(), TrainConfig(), "{'nb_filter':20}")
//        train(TradeConfig(), TrainConfig(), "{'nb_filter':12}")
//        train(TradeConfig(), TrainConfig(), "{'nb_filter':10}")
//        train(TradeConfig(), TrainConfig(), "{'nb_filter':8}")
//        train(TradeConfig(), TrainConfig(), "{'nb_filter':3}")
//        train(TradeConfig(), TrainConfig(), "{'filter_number':16}")
//        train(TradeConfig(), TrainConfig(), "{'filter_number':32}")
//        train(TradeConfig(), TrainConfig(), "{'filter_number':12}")
//        train(TradeConfig(), TrainConfig(), "{'filter_number':10}")
//        train(TradeConfig(), TrainConfig(), "{'filter_number':24}")
//        train(TradeConfig(), TrainConfig(), "{'weight_decay':5e-10}")
//        train(TradeConfig(), TrainConfig(), "{'weight_decay':5e-8}")
//        train(TradeConfig(), TrainConfig(), "{'weight_decay':5e-7}")
//        train(TradeConfig(), TrainConfig(), "{'weight_decay':5e-6}")
//        train(TradeConfig(), TrainConfig(), "{'weight_decay_last':5e-10}")
//        train(TradeConfig(), TrainConfig(), "{'weight_decay_last':5e-6}")
//        train(TradeConfig(), TrainConfig(), "{'lr_max':0.00028}")
//        train(TradeConfig(), TrainConfig(), "{'lr_max':0.00028 * 4}")
//        train(TradeConfig(), TrainConfig(), "{'lr_max':0.00028 * 8}")
//        train(TradeConfig(), TrainConfig(), "{'lr_min':0.00014}")
//        train(TradeConfig(), TrainConfig(), "{'lr_min':0.00014, 'lr_max':0.00042}")
//        train(TradeConfig(), TrainConfig(), "{'lr_beta2':0.99}")
//        train(TradeConfig(), TrainConfig(), "{'lr_beta2':0.9}")
//        train(TradeConfig(), TrainConfig(), "{'lr_beta2':0.9999}")
//        train(TradeConfig(), TrainConfig(), "{'lr_epsilon':1e-6}")
//        train(TradeConfig(), TrainConfig(), "{'lr_epsilon':1e-4}")
//        train(TradeConfig(), TrainConfig(), "{'lr_epsilon':1e-10}")
    }
}