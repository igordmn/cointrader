package com.dmi.cointrader.app.train

import com.dmi.cointrader.app.binance.testBinanceExchange
import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.numRange
import com.dmi.cointrader.app.candle.size
import com.dmi.cointrader.app.archive.Moment
import com.dmi.cointrader.app.archive.Prices
import com.dmi.cointrader.app.archive.archive
import com.dmi.cointrader.app.neural.*
import com.dmi.cointrader.app.test.TestExchange
import com.dmi.cointrader.app.trade.*
import com.dmi.util.collection.SuspendList
import com.dmi.util.collection.rangeMap
import com.dmi.util.collection.toInt
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.map
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

private typealias SetPortfolioBatch = (PortionsBatch) -> Unit

suspend fun train() = resourceContext {
    val networksFolder = Paths.get("data/networks")
    networksFolder.toFile().deleteRecursively()
    Files.createDirectory(networksFolder)
    fun netFolder(step: Int) = networksFolder.resolve(step.toString())

    val tradeConfig = TradeConfig().apply {
        saveTradeConfig(this)
    }
    val trainConfig = TrainConfig()
    val binanceExchange = testBinanceExchange().apply {
        require(trainConfig.range.endInclusive <= currentTime())
    }
    val testExchange = TestExchange(tradeConfig.assets, trainConfig.fee.toBigDecimal())
    val archive = archive(tradeConfig, binanceExchange, trainConfig.range.endInclusive)
    val jep = jep()
    val net = trainingNetwork(jep, tradeConfig)
    val trainer = networkTrainer(jep, net, trainConfig)
    val periodRange = trainConfig.range.rangeMap(tradeConfig.periods::of)
    val random = GeometricDistribution(trainConfig.geometricBias)
    val portfolios = initPortfolios(periodRange.size().toInt(), tradeConfig.assets.all.size)
    val resultsFile = networksFolder.resolve("results.txt")

    fun batches(): ReceiveChannel<TrainBatch> = produce {
        while (true) {
            val batchNums = batchNums(random, tradeConfig.historySize, trainConfig.batchSize, periodRange.numRange())
            val batch = batch(config.historySize, moments, portfolios, batchNums)
            send(batch)
        }
    }

    fun train(batch: TrainBatch): Double {
        val result = trainer.train(
                batch.portfolioMatrix(config),
                batch.historyMatrix(config),
                batch.futurePriceIncsMatrix(config)
        )
        val periodProfit = result.geometricMeanProfit
        val newPortfolios = result.newPortions.toPortfolios()
        setPortions(batch, newPortfolios)
        return periodProfit
    }

    fun saveNet(result: TrainResult) {
        net.save(netFolder(result.step))
        resultsFile.toFile().appendText(result.toString())
    }

    fun trainResult(step: Int, trainProfits: Profits, test1Profits: Profits, test2Profits: Profits): TrainResult {
        val period = tradeConfig.periods.duration
        val periodsPerDay = tradeConfig.periods.perDay().toInt()

        val trainPeriodProfit = geoMean(trainProfits)
        val trainDayProfit = trainPeriodProfit.pow(periodsPerDay)

        val test1DayProfit = test1Profits.dayly(period).let(::geoMean)
        val hourlyTest1Profits = test1Profits.hourly(period)
        val test1DownsideDeviation: Double = hourlyTest1Profits.let(::downsideDeviation)
        val test1MaximumDrawdawn: Double = hourlyTest1Profits.let(::maximumDrawdawn)

        val test2DayProfit = test2Profits.dayly(period).let(::geoMean)
        val hourlyTest2Profits = test2Profits.hourly(period)
        val test2DownsideDeviation: Double = hourlyTest2Profits.let(::downsideDeviation)
        val test2MaximumDrawdawn: Double = hourlyTest2Profits.let(::maximumDrawdawn)

        return TrainResult(
                step,
                trainDayProfit,
                TrainResult.Test(test1DayProfit, test1DownsideDeviation, test1MaximumDrawdawn),
                TrainResult.Test(test2DayProfit, test2DownsideDeviation, test2MaximumDrawdawn)
        )
    }

    batches().map(::train).chunked(trainConfig.logSteps).withIndex().consumeEach {
        val step = (it.index + 1) * trainConfig.logSteps
        val test1Profits = backTest1.invoke()
        val test2Profits = backTest2.invoke()
        saveNet(trainResult(step, it.value, test1Profits, test2Profits))
    }
}

private data class TrainResult(
        val step: Int,
        val trainDayProfit: Double,
        val test1: Test,
        val test2: Test
) {
    data class Test(val dayProfit: Double, val hourlyNegativeDeviation: Double, val hourlyMaximumDrawdawn: Double)
}

private fun setPortions(batch: TrainBatch, newPortions: List<Portions>) {
    batch.moments.forEachIndexed { i, it ->
        it.setPortfolio(newPortions[i])
    }
}

fun initPortfolio(coinCount: Int): Portions = Array(coinCount) { 1.0 / coinCount }.toList()
fun initPortfolios(size: Int, coinCount: Int) = Array(size) { initPortfolio(coinCount) }

private fun batchNums(random: GeometricDistribution, historySize: Int, batchSize: Int, limits: LongRange): LongRange {
    val firstNum = limits.first.coerceAtLeast(historySize + batchSize - 2L)
    val lastNum = limits.last
    val lastBatchNum = random.rangeSample((firstNum..lastNum).toInt()).toLong() - 1
    val firstBatchNum = lastBatchNum - batchSize + 1

    val firstBatchFirstHistoryNum = firstBatchNum - historySize + 1
    val lastBatchFutureMomentNum = lastBatchNum + 1

    return firstBatchFirstHistoryNum..lastBatchFutureMomentNum
}

private suspend fun batch(historySize: Int, moments: SuspendList<Moment>, portfolios: Array<Portions>, nums: LongRange): TrainBatch {
    fun Moment.prices(): Prices = coinIndexToCandle.map(Candle::low)
    fun priceInc(previousPrice: Double, nextPrice: Double) = nextPrice / previousPrice
    fun priceIncs(currentPrices: List<Double>, nextPrices: List<Double>): List<Double> = currentPrices.zip(nextPrices, ::priceInc)

    fun setPortfolioFun(index: Int): SetPortfolio = { portfolio: Portions ->
        portfolios[index] = portfolio
    }

    val batchMoments = moments.get(nums)
    val batchPortfolios = portfolios.sliceArray(nums.toInt())
    val batchSetPortfolios = nums.toInt().map(::setPortfolioFun)
    val batchPrices = batchMoments.map(Moment::prices)
    val batchPriceIncs = batchPrices.zipWithNext(::priceIncs)
    val indices = historySize - 1..batchMoments.size - 2

    fun trainMoment(index: Int) = TrainMoment(
            history = batchMoments.slice(index - historySize + 1..index),
            portfolio = batchPortfolios[index],
            setPortfolio = batchSetPortfolios[index],
            futurePriceIncs = batchPriceIncs[index]
    )

    return TrainBatch(indices.map(::trainMoment))
}

private class TrainBatch(
        val history: HistoryBatch,
        val portfolio: PortionsBatch,
        val futurePriceIncs: PriceIncsBatch,
        val setPortfolio: SetPortfolioBatch
)