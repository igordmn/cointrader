package com.dmi.cointrader.app.train

import com.dmi.cointrader.app.archive.HistoryBatch
import com.dmi.cointrader.app.binance.publicBinanceExchange
import com.dmi.cointrader.app.archive.Moment
import com.dmi.cointrader.app.archive.Prices
import com.dmi.cointrader.app.archive.archive
import com.dmi.cointrader.app.candle.*
import com.dmi.cointrader.app.neural.*
import com.dmi.cointrader.app.test.TestExchange
import com.dmi.cointrader.app.trade.*
import com.dmi.util.collection.SuspendList
import com.dmi.util.collection.toInt
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.map
import com.dmi.util.io.appendText
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.dmi.util.math.downsideDeviation
import com.dmi.util.math.geoMean
import com.dmi.util.math.maximumDrawdawn
import com.dmi.util.math.rangeSample
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.channels.withIndex
import org.apache.commons.math3.distribution.GeometricDistribution
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.pow

suspend fun train() = resourceContext {
    val networksFolder = Paths.get("data/networks")
    networksFolder.deleteRecursively()
    Files.createDirectory(networksFolder)
    fun netFolder(step: Int) = networksFolder.resolve(step.toString())
    val resultsFile = networksFolder.resolve("results.txt")

    val tradeConfig = TradeConfig().apply {
        saveTradeConfig(this)
    }
    val trainConfig = TrainConfig()
    val binanceExchange = publicBinanceExchange().apply {
        require(trainConfig.range.endInclusive <= currentTime())
    }
    val testExchange = TestExchange(tradeConfig.assets, trainConfig.fee.toBigDecimal())
    val archive = archive(tradeConfig, binanceExchange, trainConfig.range.endInclusive)
    val jep = jep()
    val net = trainingNetwork(jep, tradeConfig)
    val trainer = networkTrainer(jep, net)
    val periodsPerDay = tradeConfig.periods.perDay()
    val startPeriod = tradeConfig.periods.of(trainConfig.range.start)
    val endPeriod = tradeConfig.periods.of(trainConfig.range.endInclusive)
    val testStart = endPeriod.previous((periodsPerDay * (trainConfig.testDays + trainConfig.validationDays)).toInt())
    val validationStart = endPeriod.previous((periodsPerDay * trainConfig.validationDays).toInt())
    val trainRange = startPeriod until validationStart
    val testRange = testStart until validationStart
    val validationRange = validationStart until endPeriod
    val random = GeometricDistribution(trainConfig.geometricBias)
    val portfolios = initPortfolios(trainRange.size().toInt(), tradeConfig.assets.all.size)

    fun batches(): ReceiveChannel<TrainBatch> = produce {
        while (true) {
            val batchPeriods = random.batchPeriods(tradeConfig.historySize, trainConfig.batchSize, trainRange)
            val batch = batch(tradeConfig.historySize, moments, portfolios, batchPeriods)
            send(batch)
        }
    }

    fun train(batch: TrainBatch): Double {
        val (newPortions, geometricMeanProfit) = trainer.train(batch.currentPortfolio, batch.history, batch.futurePriceIncs, batch.fees)
        batch.periods.nums().toInt().forEach {
            portfolios[it] = newPortions[it]
        }
        return geometricMeanProfit
    }

    fun saveNet(result: TrainResult) {
        net.save(netFolder(result.step))
        resultsFile.appendText(result.toString())
    }

    fun trainResult(step: Int, trainProfits: Profits, testResults: List<TradeResult>, validationResults: List<TradeResult>): TrainResult {
        val period = tradeConfig.periods.duration

        fun trainTestResult(tradeResults: List<TradeResult>): TrainResult.Test {
            val profits = tradeResults.capitals().profits()
            val dayProfit = profits.dayly(period).let(::geoMean)
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

    batches().map(::train).chunked(trainConfig.logSteps).withIndex().consumeEach {
        val step = (it.index + 1) * trainConfig.logSteps
        val testProfits = performTestTrades(testRange, tradeConfig, net, archive, testExchange)
        val validationProfits = performTestTrades(validationRange, tradeConfig, net, archive, testExchange)
        saveNet(trainResult(step, it.value, testProfits, validationProfits))
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

fun initPortfolios(size: Int, coinNumber: Int) = Array(size) { initPortfolio(coinNumber) }
fun initPortfolio(coinNumber: Int): Portions = Array(coinNumber) { 1.0 / coinNumber }.toList()

private fun GeometricDistribution.batchPeriods(historySize: Int, batchSize: Int, allRange: PeriodRange): PeriodRange {
    val firstNum = allRange.start.num.coerceAtLeast(historySize + batchSize - 2L)
    val lastNum = allRange.endInclusive.num
    val lastBatchNum = rangeSample((firstNum..lastNum).toInt()).toLong() - 1
    val firstBatchNum = lastBatchNum - batchSize + 1

    val firstBatchFirstHistoryNum = firstBatchNum - historySize + 1
    val lastBatchFutureMomentNum = lastBatchNum + 1

    return Period(firstBatchFirstHistoryNum)..Period(lastBatchFutureMomentNum)
}

private suspend fun batch(historySize: Int, moments: SuspendList<Moment>, portfolios: Array<Portions>, periods: PeriodRange): TrainBatch {
    fun Moment.prices(): Prices = coinIndexToCandle.map(Candle::low)
    fun priceInc(previousPrice: Double, nextPrice: Double) = nextPrice / previousPrice
    fun priceIncs(currentPrices: List<Double>, nextPrices: List<Double>): List<Double> = currentPrices.zip(nextPrices, ::priceInc)

    val nums = periods.nums()
    val batchMoments = moments.get(nums)
    val batchPortfolios = portfolios.sliceArray(nums.toInt())
    val batchPrices = batchMoments.map(Moment::prices)
    val batchPriceIncs = batchPrices.zipWithNext(::priceIncs)
    val indices = historySize - 1..batchMoments.size - 2

    fun trainMoment(index: Int) = TrainMoment(
            history = batchMoments.slice(index - historySize + 1..index),
            portfolio = batchPortfolios[index],
            setPortfolio = batchSetPortfolios[index],
            futurePriceIncs = batchPriceIncs[index]
    )

    return TrainBatch(periodsindices.map(::trainMoment))
}

private class TrainBatch(
        val periods: PeriodRange,
        val currentPortfolio: PortionsBatch,
        val history: HistoryBatch,
        val futurePriceIncs: PriceIncsBatch,
        val fees: FeesBatch
)