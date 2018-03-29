package com.dmi.cointrader.train

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.neural.*
import com.dmi.cointrader.test.TestExchange
import com.dmi.cointrader.trade.*
import com.dmi.util.collection.contains
import com.dmi.util.collection.set
import com.dmi.util.collection.size
import com.dmi.util.collection.slice
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.infiniteChannel
import com.dmi.util.concurrent.map
import com.dmi.util.io.appendText
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.dmi.util.math.downsideDeviation
import com.dmi.util.math.geoMean
import com.dmi.util.math.maximumDrawdawn
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.channels.withIndex
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.pow

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
    val testExchange = TestExchange(tradeConfig.assets, trainConfig.fee.toBigDecimal())
    val periods = trainConfig.range.periods(tradeConfig.periodSpace)
    val archive = archive(
            tradeConfig.periodSpace, tradeConfig.assets, binanceExchange, periods.last,
            reloadCount = tradeConfig.archiveReloadCount
    )
    val jep = jep()
    val net = trainingNetwork(jep, tradeConfig)
    val trainer = networkTrainer(jep, net, trainConfig.fee)
    val (trainPeriods, testPeriods, validationPeriods) = periods.splitForTrain(tradeConfig, trainConfig)
    val batches = TrainBatches(trainPeriods, trainConfig.batchSize, tradeConfig, archive)

    fun train(batch: TrainBatch): Double {
        val (newPortions, geometricMeanProfit) = trainer.train(batch.currentPortfolio, batch.history)
        batch.setCurrentPortfolio(newPortions)
        return geometricMeanProfit
    }

    fun saveNet(result: TrainResult) {
        net.save(netFolder(result.step))
        resultsFile.appendText(result.toString())
    }

    saveTradeConfig(tradeConfig)
    batches.channel()
            .map(::train)
            .chunked(trainConfig.logSteps)
            .withIndex()
            .consumeEach {
                saveNet(trainResult(
                        space = tradeConfig.periodSpace,
                        step = it.index * trainConfig.logSteps,
                        trainProfits = it.value,
                        testResults = performTestTrades(testPeriods, tradeConfig, net, archive, testExchange),
                        validationResults = performTestTrades(validationPeriods, tradeConfig, net, archive, testExchange)
                ))
            }
}