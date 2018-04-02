package com.dmi.cointrader.train

import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.periods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.neural.jep
import com.dmi.cointrader.neural.networkTrainer
import com.dmi.cointrader.neural.trainingNetwork
import com.dmi.cointrader.trade.*
import com.dmi.util.collection.contains
import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.XYChart
import org.knowm.xchart.style.markers.None
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Paths

suspend fun train() = resourceContext {
    val resultsDir = Paths.get("data/results")
    resultsDir.deleteRecursively()
    Files.createDirectory(resultsDir)

    val networksDir = resultsDir.resolve("networks")
    Files.createDirectory(networksDir)
    fun netDir(step: Int) = networksDir.resolve(step.toString())

    val chartsDir = resultsDir.resolve("charts")
    Files.createDirectory(chartsDir)
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

    fun saveChart(result: TrainResult) {
        val chart = XYChart(1280, 720).apply {
            addSeries("Capital", result.dayProfitsChart.x, result.dayProfitsChart.y).apply {
                marker = None()
                markerColor = Color(0, 0, 0, 0)
            }
            styler.antiAlias = true
            styler.plotMargin = 1
        }

        chartFile(result.step).toFile().outputStream().buffered().use {
            BitmapEncoder.saveBitmap(chart, it, BitmapEncoder.BitmapFormat.PNG)
        }
    }

    fun saveNet(result: TrainResult) {
        net.save(netDir(result.step))
        saveChart(result)
        resultsLogFile.appendLine(result.toString())
        println(result.toString())
    }

    saveTradeConfig(tradeConfig)

    var trainProfits = ArrayList<Double>(trainConfig.logSteps)
    val logResults = ArrayList<TrainResult>()
    batches.channel().consumeEachIndexed { (i, it) ->
        val (newPortions, trainProfit) = trainer.train(it.currentPortfolio, it.history)
        it.setCurrentPortfolio(newPortions)
        trainProfits.add(trainProfit)
        if (i % trainConfig.logSteps == 0) {
            val result = trainResult(
                    space = tradeConfig.periodSpace,
                    tradePeriods = tradeConfig.tradePeriods.size,
                    step = i,
                    movingAverageCount = trainConfig.logMovingAverageCount,
                    previousResults = logResults,
                    trainProfits = trainProfits,
                    testCapitals = performTestTradesFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee),
                    validationCapitals = performTestTradesFast2(validationPeriods, tradeConfig, net, archive, trainConfig.fee)
            )
            logResults.add(result)
            saveNet(result)
            trainProfits = ArrayList(trainConfig.logSteps)
        }
    }
}