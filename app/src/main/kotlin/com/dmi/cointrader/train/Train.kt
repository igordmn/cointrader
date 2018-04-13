package com.dmi.cointrader.train

import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.periods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.info.saveChart
import com.dmi.cointrader.neural.NeuralNetwork
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
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import kotlinx.coroutines.experimental.channels.take
import java.nio.file.Files.createDirectory
import java.nio.file.Paths

suspend fun train() = resourceContext {
    val resultsDir = Paths.get("data/results")
    resultsDir.deleteRecursively()
    createDirectory(resultsDir)

    val networksDir = resultsDir.resolve("networks")
    createDirectory(networksDir)
    fun netDir(repeat: Int, step: Int) = networksDir.resolve(repeat.toString()).resolve(step.toString())

    val chartsDir = resultsDir.resolve("charts")
    createDirectory(chartsDir)
    fun chartFile(repeat: Int, step: Int) = chartsDir.resolve(repeat.toString()).resolve("$step.png")

    val resultsLogFile = resultsDir.resolve("results.log")

    val tradeConfig = TradeConfig()
    val trainConfig = TrainConfig()
    val binanceExchange = binanceExchangeForInfo()
    require(trainConfig.range in tradeConfig.periodSpace.start..binanceExchange.currentTime())
    val periods = trainConfig.range.periods(tradeConfig.periodSpace)
    val archive = archive(
            tradeConfig.periodSpace, tradeConfig.assets, binanceExchange, periods.last,
            reloadCount = tradeConfig.archiveReloadPeriods
    )
    val jep = jep()
    val (trainPeriods, testPeriods, validationPeriods) = periods.prepareForTrain(tradeConfig, trainConfig)

    PlatformImpl.startup({})
    saveTradeConfig(tradeConfig)

    repeat(trainConfig.repeats) { repeat ->
        var trainProfits = ArrayList<Double>(trainConfig.logSteps)
        val results = ArrayList<TrainResult>()
        val batches = trainBatches(archive, trainPeriods, tradeConfig, trainConfig)
        val net = trainingNetwork(jep, tradeConfig)
        val trainer = networkTrainer(jep, net, trainConfig.fee)

        fun saveNet(result: TrainResult) {
            net.save(netDir(repeat, result.step))
            saveChart(result.tests[0].chartData, chartFile(repeat, result.step))
            resultsLogFile.appendLine(result.toString())
            println(result.toString())
        }

        batches.channel().take(trainConfig.steps).consumeEachIndexed { (i, it) ->
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
                                performTestTradesAllInFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee),
                                performTestTradesPartialFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee)
                        )
                )
                results.add(result)
                saveNet(result)
                trainProfits = ArrayList(trainConfig.logSteps)
            }
        }
    }
}