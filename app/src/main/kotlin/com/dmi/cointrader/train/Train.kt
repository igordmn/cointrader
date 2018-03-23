package com.dmi.cointrader.train

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.binance.publicBinanceExchange
import com.dmi.cointrader.neural.*
import com.dmi.cointrader.test.TestExchange
import com.dmi.cointrader.app.trade.*
import com.dmi.cointrader.trade.*
import com.dmi.util.collection.set
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.map
import com.dmi.util.io.appendText
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.dmi.util.math.downsideDeviation
import com.dmi.util.math.geoMean
import com.dmi.util.math.maximumDrawdawn
import com.dmi.util.math.sampleIn
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
    val (trainRange, testRange, validationRange) = ranges(tradeConfig, trainConfig)
    val random = GeometricDistribution(trainConfig.geometricBias)
    val portfolios = initPortfolios(trainRange.endInclusive.num + 1, tradeConfig.assets.all.size)

    fun batches(): ReceiveChannel<TrainBatch> = produce {
        while (true) {
            send(batch(random, trainRange, tradeConfig.historySize, trainConfig.batchSize, trainConfig.fee, archive, portfolios))
        }
    }

    fun train(batch: TrainBatch): Double {
        val (newPortions, geometricMeanProfit) = trainer.train(batch.currentPortfolio, batch.history, batch.futurePriceIncs, batch.fees)
        batch.setCurrentPortfolio(newPortions)
        return geometricMeanProfit
    }

    fun saveNet(result: TrainResult) {
        net.save(netFolder(result.step))
        resultsFile.appendText(result.toString())
    }

    fun trainResult(step: Int, trainProfits: Profits, testResults: List<TradeResult>, validationResults: List<TradeResult>): TrainResult {
        val periodsPerDay = tradeConfig.periods.perDay()
        val period = tradeConfig.periods.duration

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

    batches()
            .map(::train)
            .chunked(trainConfig.logSteps)
            .withIndex()
            .consumeEach {
                val step = it.index * trainConfig.logSteps
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

private fun ranges(tradeConfig: TradeConfig, trainConfig: TrainConfig): Triple<PeriodRange, PeriodRange, PeriodRange> {
    val periods = tradeConfig.periods
    val timeRange = trainConfig.range
    val periodsPerDay = periods.perDay()
    val extraNeededFirstNums = tradeConfig.historySize + trainConfig.batchSize - 2
    val extraNeededLastNums = 2  // -2st period for current tradeTimePrice, -1st period for next tradeTimePrice
    val start = periods.of(timeRange.start).next(extraNeededFirstNums)
    val end = periods.of(timeRange.endInclusive).previous(extraNeededLastNums)
    val testStart = end.previous((periodsPerDay * (trainConfig.testDays + trainConfig.validationDays)).toInt())
    val validationStart = end.previous((periodsPerDay * trainConfig.validationDays).toInt())
    val trainRange = start until validationStart
    val testRange = testStart until validationStart
    val validationRange = validationStart until end
    return Triple(trainRange, testRange, validationRange)
}

private suspend fun batch(
        random: GeometricDistribution,
        range: PeriodRange,
        historySize: Int,
        batchSize: Int,
        exchangeFee: Double,
        archive: Archive,
        portfolios: Array<Portions>
): TrainBatch {
    fun Candle.tradeTimePrice() = (tradeTimeAsk + tradeTimeBid) / 2
    fun Candle.tradeTimeFee() = 1 - (1 - exchangeFee) * (tradeTimeBid / tradeTimePrice())
    fun priceInc(currentPrice: Double, nextPrice: Double) = nextPrice / currentPrice

    fun lastNums(): IntRange {
        val last = random.sampleIn(range.nums())
        val first = last - batchSize + 1
        return first..last
    }

    val lastNums = lastNums()
    val allNums = lastNums.start - historySize + 1..lastNums.endInclusive + 2
    val moments = archive.historyAt(allNums.toPeriods())
    val indices = (0 until batchSize).map { it + historySize - 1 }

    return TrainBatch(
            setCurrentPortfolio = { portfolios.set(lastNums, it) },
            currentPortfolio = portfolios.slice(lastNums),
            history = indices.map {
                moments.slice(it - historySize + 1..it)
            },
            futurePriceIncs = indices.map {
                val prices = moments[it + 1].coinIndexToCandle.map(Candle::tradeTimePrice)
                val nextPrices = moments[it + 2].coinIndexToCandle.map(Candle::tradeTimePrice)
                prices.zip(nextPrices, ::priceInc)
            },
            fees = indices.map {
                moments[it + 1].coinIndexToCandle.map(Candle::tradeTimeFee)
            }
    )
}

data class TrainHistory()

private class TrainBatch(
        val setCurrentPortfolio: (PortionsBatch) -> Unit,
        val currentPortfolio: PortionsBatch,
        val history: HistoryBatch,
        val futurePriceIncs: PriceIncsBatch,
        val fees: FeesBatch
)