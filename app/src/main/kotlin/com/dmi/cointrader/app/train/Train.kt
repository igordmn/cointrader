package com.dmi.cointrader.app.train

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.periodNum
import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.test.BackTest
import com.dmi.cointrader.app.test.dayly
import com.dmi.cointrader.app.test.hourly
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.mapAsync
import com.dmi.util.io.SyncList
import com.dmi.util.collection.toInt
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.map
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.math.*
import com.dmi.cointrader.app.binance.api.binanceAPI
import com.dmi.cointrader.app.history.Prices
import com.dmi.cointrader.app.neural.*
import jep.Jep
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking
import org.apache.commons.math3.distribution.GeometricDistribution
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.pow

private val netsPath = Paths.get("data/nets")

private typealias SetPortfolioBatch = (PortionsBatch) -> Unit

suspend fun train() {

}

fun main(args: Array<String>) {
    runBlocking {
        netsPath.toFile().deleteRecursively()
        Files.createDirectory(netsPath)

        val config = TrainConfig()

        val api = binanceAPI()
        val constants = BinanceConstants()
        val currentTime = MemoryAtom(config.trainEndTime)

        fun coinLog(coin: String) = object : SyncList.Log<Trade> {
            override fun itemsAppended(items: List<Trade>, indices: LongRange) {
                val lastTradeTime = items.last().time
                println("$coin $lastTradeTime")
            }
        }

        val coinToTrades = coinToCachedBinanceTrades(config, constants, api, currentTime, ::coinLog)
        val moments = cachedMoments(config, coinToTrades, currentTime)

        println("Download trades")
        coinToTrades.mapAsync {
            it.sync()
        }

        println("Make moments")
        moments.sync()

        val startPeriodNum = periodNum(config.startTime, config.period, config.trainStartTime)
        val endPeriodNum = periodNum(config.startTime, config.period, config.trainEndTime).coerceAtMost(moments.size())

        jep().use { jep ->
            network(jep, config).use { net ->
                val backTest1 = BackTest(net, moments, config, config.trainTest1Days)
                val backTest2 = BackTest(net, moments, config, config.trainTest2Days)
                trainer(jep, config, net).use { trainer ->
                    train(backTest1, backTest2, net, trainer, config, moments, startPeriodNum until endPeriodNum)
                }
            }
        }
    }
}

private suspend fun train(backTest1: BackTest, backTest2: BackTest, network: NeuralNetwork, trainer: NeuralTrainer, config: Config, moments: SuspendList<Moment>, nums: LongRange) {
    val random = GeometricDistribution(config.trainGeometricBias)
    val portfolios = initPortfolios(moments.size().toInt(), config.altCoins.size + 1)  // with mainCoin
    val resultsFile = netsPath.resolve("results.txt")

    fun batches(): ReceiveChannel<TrainBatch> = produce {
        while (true) {
            val batchNums = batchNums(random, config, nums)
            val batch = batch(config.historyCount, moments, portfolios, batchNums)
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

    val funs = object {
        suspend fun saveNet(info: TrainInfo) {
            network.save(netsPath.resolve(info.step.toString()))
            resultsFile.toFile().appendText(info.toString())
        }
    }

    fun collectInfo(step: Int, trainProfits: List<Double>, test1Profits: List<Double>, test2Profits: List<Double>): TrainInfo {
        val trainPeriodProfit = geoMean(trainProfits)
        val periodsPerDay = (MILLIS_PER_DAY / config.period.toMillis()).toInt()
        val trainDayProfit = trainPeriodProfit.pow(periodsPerDay)

        val test1DayProfit = test1Profits.dayly(config).let(::geoMean)
        val hourlyTest1Profits = test1Profits.hourly(config)
        val test1DownsideDeviation: Double = hourlyTest1Profits.let(::downsideDeviation)
        val test1MaximumDrawdawn: Double = hourlyTest1Profits.let(::maximumDrawdawn)

        val test2DayProfit = test2Profits.dayly(config).let(::geoMean)
        val hourlyTest2Profits = test2Profits.hourly(config)
        val test2DownsideDeviation: Double = hourlyTest2Profits.let(::downsideDeviation)
        val test2MaximumDrawdawn: Double = hourlyTest2Profits.let(::maximumDrawdawn)

        return TrainInfo(
                step,

                trainDayProfit,

                test1DayProfit,
                test1DownsideDeviation,
                test1MaximumDrawdawn,

                test2DayProfit,
                test2DownsideDeviation,
                test2MaximumDrawdawn
        )
    }

    batches().map(::train).chunked(config.trainLogSteps).withIndex().consumeEach {
        val step = (it.index + 1) * config.trainLogSteps
        val test1Profits = backTest1.invoke()
        val test2Profits = backTest2.invoke()
        funs.saveNet(collectInfo(step, it.value, test1Profits, test2Profits))
    }
}

private data class TrainInfo(
        val step: Int,

        val trainDayProfit: Double,

        val test1DayProfit: Double,
        val test1HourlyNegativeDeviation: Double,
        val test1HourlyMaximumDrawdawn: Double,

        val test2DayProfit: Double,
        val test2HourlyNegativeDeviation: Double,
        val test2HourlyMaximumDrawdawn: Double
)

private fun setPortions(batch: TrainBatch, newPortions: List<Portions>) {
    batch.moments.forEachIndexed { i, it ->
        it.setPortfolio(newPortions[i])
    }
}

fun initPortfolio(coinCount: Int): Portions = Array(coinCount) { 1.0 / coinCount }.toList()
fun initPortfolios(size: Int, coinCount: Int) = Array(size) { initPortfolio(coinCount) }

private suspend fun batchNums(random: GeometricDistribution, config: Config, limits: LongRange): LongRange {
    val firstNum = limits.first.coerceAtLeast(config.historyCount + config.trainBatchSize - 2L)
    val lastNum = limits.last
    val lastBatchNum = random.rangeSample((firstNum..lastNum).toInt()).toLong() - 1
    val firstBatchNum = lastBatchNum - config.trainBatchSize + 1

    val firstBatchFirstHistoryNum = firstBatchNum - config.historyCount + 1
    val lastBatchFutureMomentNum = lastBatchNum + 1

    return firstBatchFirstHistoryNum..lastBatchFutureMomentNum
}

private suspend fun batch(historyCount: Int, moments: SuspendList<Moment>, portfolios: Array<Portions>, nums: LongRange): TrainBatch {
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
    val indices = historyCount - 1..batchMoments.size - 2

    fun trainMoment(index: Int) = TrainMoment(
            history = batchMoments.slice(index - historyCount + 1..index),
            portfolio = batchPortfolios[index],
            setPortfolio = batchSetPortfolios[index],
            futurePriceIncs = batchPriceIncs[index]
    )

    return TrainBatch(indices.map(::trainMoment))
}

private fun network(jep: Jep, config: Config) = NeuralNetwork.init(
        jep,
        NeuralNetwork.Config(1 + config.altCoins.size, config.historyCount, 3),
        gpuMemoryFraction = 0.5
)

private fun trainer(jep: Jep, config: Config, net: NeuralNetwork) = NeuralTrainer(
        jep,
        net,
        NeuralTrainer.Config(config.fee.toDouble())
)

private class TrainBatch(
        val history: HistoryBatch,
        val portfolio: PortionsBatch,
        val futurePriceIncs: PriceIncsBatch,
        val setPortfolio: SetPortfolioBatch
)