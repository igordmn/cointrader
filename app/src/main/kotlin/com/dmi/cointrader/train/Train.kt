package com.dmi.cointrader.train

import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.periods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.neural.jep
import com.dmi.cointrader.neural.networkTrainer
import com.dmi.cointrader.neural.trainingNetwork
import com.dmi.cointrader.test.TestExchange
import com.dmi.cointrader.trade.*
import com.dmi.util.collection.contains
import com.dmi.util.io.appendLine
import com.dmi.util.io.appendText
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.dmi.util.math.geoMean
import kotlinx.coroutines.experimental.channels.consumeEachIndexed
import java.nio.file.Files
import java.nio.file.Paths

suspend fun train() = resourceContext {
    val networksFolder = Paths.get("data/networks")
    networksFolder.deleteRecursively()
    Files.createDirectory(networksFolder)
    fun netFolder(step: Int) = networksFolder.resolve(step.toString())
    val resultsFile = networksFolder.resolve("results.log")

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

    fun saveNet(result: TrainResult) {
        net.save(netFolder(result.step))
        resultsFile.appendLine(result.toString())
        println(result.toString())
    }

    saveTradeConfig(tradeConfig)

    var trainProfits = ArrayList<Double>(trainConfig.logSteps)
    batches.channel().consumeEachIndexed { (i, it) ->
        val (newPortions, trainProfit) = trainer.train(it.currentPortfolio, it.history)
        it.setCurrentPortfolio(newPortions)
        trainProfits.add(trainProfit)
        if (i % trainConfig.logSteps == 0) {
            saveNet(trainResult(
                    space = tradeConfig.periodSpace,
                    tradePeriods = tradeConfig.tradePeriods.size,
                    step = i,
                    trainProfits = trainProfits,
                    testProfits = performTestTradesFast(validationPeriods, tradeConfig, net, archive, trainConfig.fee).profits(),
                    validationProfits = performTestTradesFast2(validationPeriods, tradeConfig, net, archive, trainConfig.fee).profits()
            ))
            trainProfits = ArrayList(trainConfig.logSteps)
        }
    }
}