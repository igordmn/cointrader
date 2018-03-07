package com.dmi.cointrader.main

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.candleNum
import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.NeuralTrainer
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.collection.SuspendList
import com.dmi.util.collection.subList
import com.dmi.util.concurrent.mapAsync
import com.dmi.util.io.SyncList
import com.dmi.util.collection.toInt
import com.dmi.util.math.DoubleMatrix2D
import com.dmi.util.math.DoubleMatrix4D
import com.dmi.util.math.rangeSample
import exchange.binance.BinanceConstants
import exchange.binance.api.binanceAPI
import jep.Jep
import kotlinx.coroutines.experimental.runBlocking
import main.test.Config
import org.apache.commons.math3.distribution.GeometricDistribution
import python.jep
import java.nio.file.Files
import java.nio.file.Paths

private val netsPath = Paths.get("data/nets")

private typealias History = List<Moment>
private typealias Portfolio = List<Double>
private typealias SetPortfolio = (Portfolio) -> Unit
private typealias Prices = List<Double>
private typealias PricesIncs = List<Double>

fun main(args: Array<String>) {
    runBlocking {
        netsPath.toFile().deleteRecursively()
        Files.createDirectory(netsPath)

        val config = Config()

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

        val startPeriodNum = candleNum(config.startTime, config.period, config.trainStartTime)
        val endPeriodNum = candleNum(config.startTime, config.period, config.trainEndTime).coerceAtMost(moments.size())

        jep().use { jep ->
            network(jep, config).use { net ->
                trainer(jep, config, net).use { trainer ->
                    train(trainer, config, moments, startPeriodNum until endPeriodNum)
                }
            }
        }
    }
}

private suspend fun train(trainer: NeuralTrainer, config: Config, moments: SuspendList<Moment>, nums: LongRange): Any {
    val random = GeometricDistribution(config.trainGeometricBias)
    val portfolios = initPortfolios(moments.size().toInt(), config.altCoins.size + 1)  // with mainCoin

    repeat(config.trainSteps) {
        val batchNums = batchNums(random, config, nums)
        val batch = batch(config.historyCount, moments, portfolios, batchNums)
        val result = trainer.train(
                batch.portfolioMatrix(config),
                batch.historyMatrix(config),
                batch.futurePriceIncsMatrix(config)
        )
        val meanProfit = result.geometricMeanProfit
        val newPortfolios = result.newPortfolios.portfolios(config)
        setPortions(batch, newPortfolios)
    }
}

private fun setPortions(batch: TrainBatch, newPortions: List<Portfolio>) {
    batch.moments.forEachIndexed { i, it ->
        it.setPortfolio(newPortions[i])
    }
}

fun initPortfolio(coinCount: Int): Portfolio = Array(coinCount) { 1.0 / coinCount }.toList()
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

private suspend fun batch(historyCount: Int, moments: SuspendList<Moment>, portfolios: Array<Portfolio>, nums: LongRange): TrainBatch {
    fun Moment.prices(): Prices = coinIndexToCandle.map(Candle::low)
    fun priceInc(previousPrice: Double, nextPrice: Double) = nextPrice / previousPrice
    fun priceIncs(currentPrices: List<Double>, nextPrices: List<Double>): List<Double> = currentPrices.zip(nextPrices, ::priceInc)

    fun setPortfolioFun(index: Int): SetPortfolio = { portfolio: Portfolio ->
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

private class TrainBatch(val moments: List<TrainMoment>)

private class TrainMoment(val history: History, val portfolio: Portfolio, val setPortfolio: SetPortfolio, val futurePriceIncs: PricesIncs)

private fun TrainBatch.historyMatrix(config: Config): DoubleMatrix4D {
    fun Candle.indicator(index: Int) = when (index) {
        0 -> close
        1 -> high
        2 -> low
        else -> throw UnsupportedOperationException()
    }

    fun value(b: Int, c: Int, h: Int, i: Int) = moments[b].history[h].coinIndexToCandle[c].indicator(i)
    return DoubleMatrix4D(config.trainBatchSize, 1 + config.altCoins.size, config.historyCount, 3, ::value)
}

private fun TrainBatch.portfolioMatrix(config: Config): DoubleMatrix2D {
    fun value(b: Int, c: Int) = moments[b].portfolio[c]
    return DoubleMatrix2D(config.trainBatchSize, 1 + config.altCoins.size, ::value)
}

private fun TrainBatch.futurePriceIncsMatrix(config: Config): DoubleMatrix2D {
    fun value(b: Int, c: Int) = moments[b].futurePriceIncs[c]
    return DoubleMatrix2D(config.trainBatchSize, 1 + config.altCoins.size, ::value)
}

private fun DoubleMatrix2D.portfolios(config: Config): List<Portfolio> {
    val portfolios = ArrayList<Portfolio>(config.trainBatchSize)
    (0 until config.trainBatchSize).forEach { b ->
        val portfolio = ArrayList<Double>(1 + config.altCoins.size)
        (0 until 1 + config.altCoins.size).forEach { c ->
            portfolio.add(this[b, c])
        }
    }
    return portfolios
}