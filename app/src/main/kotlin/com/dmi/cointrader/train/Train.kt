package com.dmi.cointrader.train

import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.periods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.info.saveChart
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
import java.nio.file.Files.createDirectory
import java.nio.file.Paths

suspend fun train() = resourceContext {
    val resultsDir = Paths.get("data/results")
    resultsDir.deleteRecursively()
    createDirectory(resultsDir)

    val networksDir = resultsDir.resolve("networks")
    createDirectory(networksDir)
    fun netDir(step: Int) = networksDir.resolve(step.toString())

    val chartsDir = resultsDir.resolve("charts")
    createDirectory(chartsDir)
    fun chartFile(step: Int) = chartsDir.resolve("$step.png")

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
    val net = trainingNetwork(jep, tradeConfig)
    val trainer = networkTrainer(jep, net, trainConfig.fee)
    val (trainPeriods, testPeriods, validationPeriods) = periods.prepareForTrain(tradeConfig, trainConfig)
    val batches = trainBatches(archive, trainPeriods, tradeConfig, trainConfig)

    PlatformImpl.startup({})

    fun saveNet(result: TrainResult) {
        net.save(netDir(result.step))
        saveChart(result.tests[0].chartData, chartFile(result.step))
        resultsLogFile.appendLine(result.toString())
        println(result.toString())
    }

    saveTradeConfig(tradeConfig)

    var trainProfits = ArrayList<Double>(trainConfig.logSteps)
    val results = ArrayList<TrainResult>()
    batches.channel().consumeEachIndexed { (i, it) ->
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