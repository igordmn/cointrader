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
    fun initPortfolio(coinNumber: Int): DoubleArray = DoubleArray(coinNumber) { 1.0 / coinNumber }
    fun initPortfolios(size: Int, coinNumber: Int) = Array(size) { initPortfolio(coinNumber) }

    val networksFolder = Paths.get("data/networks")
    networksFolder.deleteRecursively()
    Files.createDirectory(networksFolder)
    fun netFolder(step: Int) = networksFolder.resolve(step.toString())
    val resultsFile = networksFolder.resolve("results.txt")

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
    val (trainPeriods, testPeriods, validationPeriods) = run {
        val space = tradeConfig.periodSpace
        val testSize = (trainConfig.testDays * space.periodsPerDay()).toInt()
        val validationSize = (trainConfig.validationDays * space.periodsPerDay()).toInt()
        val all = periods.clampForTradedHistory(tradeConfig).tradePeriods(tradeConfig.tradePeriods)
        val size = all.size()
        Triple(
                all.slice(0 until size - validationSize),
                all.slice(size - testSize until size - validationSize),
                all.slice(size - validationSize until size)
        )
    }
    val portfolios = initPortfolios(trainPeriods.size(), tradeConfig.assets.all.size).also {
        require(trainPeriods.first == 0)
    }

    val batches = object {
        val random = Random(867979346)

        suspend fun randomBatch(): TrainBatch {
            val startIndex = random.nextInt(trainPeriods.size() - trainConfig.batchSize)
            val endIndex = startIndex + trainConfig.batchSize
            val indices = startIndex until endIndex
            val batchPeriods = trainPeriods.slice(indices)
            val portfolio = portfolios.slice(indices).map { it.toList() }
            fun setPortfolio(portfolio: PortionsBatch) = portfolios.set(indices, portfolio.map { it.toDoubleArray() }.toTypedArray())
            val history = tradedHistories(tradeConfig, archive, batchPeriods).toList()
            return TrainBatch(portfolio, ::setPortfolio, history)
        }

        fun channel(): ReceiveChannel<TrainBatch> = infiniteChannel { randomBatch() }
    }


    fun train(batch: TrainBatch): Double {
        val (newPortions, geometricMeanProfit) = trainer.train(batch.currentPortfolio, batch.history)
        batch.setCurrentPortfolio(newPortions)
        return geometricMeanProfit
    }

    fun saveNet(result: TrainResult) {
        net.save(netFolder(result.step))
        resultsFile.appendText(result.toString())
    }

    fun trainResult(step: Int, trainProfits: Profits, testResults: List<TradeResult>, validationResults: List<TradeResult>): TrainResult {
        val periodsPerDay = tradeConfig.periodSpace.periodsPerDay()
        val period = tradeConfig.periodSpace.duration

        fun trainTestResult(tradeResults: List<TradeResult>): TrainResult.Test {
            val profits = tradeResults.capitals().profits()
            val dayProfit = profits.daily(period).let(::geoMean)
            val hourlyProfits = profits.hourly(period)
            val downsideDeviation: Double = hourlyProfits.let(::downsideDeviation)
            val maximumDrawdawn: Double = hourlyProfits.let(::maximumDrawdawn)
            return TrainResult.Test(dayProfit, downsideDeviation, maximumDrawdawn)
        }

        return TrainResult(
                step,
                geoMean(trainProfits).pow(periodsPerDay),
                trainTestResult(testResults),
                trainTestResult(validationResults)
        )
    }

    saveTradeConfig(tradeConfig)
    batches.channel()
            .map(::train)
            .chunked(trainConfig.logSteps)
            .withIndex()
            .consumeEach {
                saveNet(trainResult(
                        step = it.index * trainConfig.logSteps,
                        trainProfits = it.value,
                        testResults = performTestTrades(testPeriods, tradeConfig, net, archive, testExchange),
                        validationResults =performTestTrades(validationPeriods, tradeConfig, net, archive, testExchange)
                ))
            }
}

private data class TrainResult(
        val step: Int,
        val trainDayProfit: Double,
        val test: Test,
        val validation: Test
) {
    data class Test(val dayProfit: Double, val hourlyNegativeDeviation: Double, val hourlyMaximumDrawdawn: Double)
}

private class TrainBatch(
        val currentPortfolio: PortionsBatch,
        val setCurrentPortfolio: (PortionsBatch) -> Unit,
        val history: TradedHistoryBatch
)