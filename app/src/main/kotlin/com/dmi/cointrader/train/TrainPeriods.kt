package com.dmi.cointrader.train

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.neural.PortionsBatch
import com.dmi.cointrader.neural.TradedHistoryBatch
import com.dmi.cointrader.neural.clampForTradedHistory
import com.dmi.cointrader.neural.tradedHistories
import com.dmi.cointrader.HistoryPeriods
import com.dmi.cointrader.TradeConfig
import com.dmi.util.collection.*
import com.dmi.util.concurrent.infiniteChannel
import com.dmi.util.math.limitSample
import kotlinx.coroutines.experimental.channels.toList
import org.apache.commons.math3.distribution.GeometricDistribution

fun PeriodRange.prepareForTrain(tradeConfig: TradeConfig, trainConfig: TrainConfig) = this
        .clampForTradedHistory(tradeConfig.historyPeriods, tradeConfig.tradePeriods.delay)
        .tradePeriods(tradeConfig.tradePeriods.size)
        .splitForTrain(tradeConfig.periodSpace.periodsPerDay(), trainConfig.testDays, trainConfig.validationDays)

fun PeriodProgression.splitForTrain(
        periodsPerDay: Double,
        testDays: Double,
        validationDays: Double
): TrainPeriods {
    val testSize = (testDays * periodsPerDay / step).toInt()
    val validationSize = (validationDays * periodsPerDay / step).toInt()
    val size = size()
    return TrainPeriods(
            train = slice(0 until size - validationSize),
            test = slice(size - testSize until size),
            validation = slice(size - validationSize until size)
    )
}

data class TrainPeriods(val train: PeriodProgression, val test: PeriodProgression, val validation: PeriodProgression)

fun trainBatches(
        archive: SuspendList<Spreads>,
        trainPeriods: PeriodProgression,
        tradeConfig: TradeConfig,
        trainConfig: TrainConfig
) = TrainBatches(
        archive,
        trainPeriods,
        trainConfig.batchSize,
        tradeConfig.assets.all.size,
        tradeConfig.historyPeriods,
        tradeConfig.tradePeriods.delay,
        trainConfig.tradePeriodGeometricBias
)

class TrainBatches(
        private val archive: SuspendList<Spreads>,
        private val periods: PeriodProgression,
        private val batchSize: Int,
        private val assetsSize: Int,
        private val historyPeriods: HistoryPeriods,
        private val tradeDelayPeriods: Int,
        tradePeriodGeometricBias: Double = 5e-7
) {
    private fun initPortfolio(): DoubleArray = DoubleArray(assetsSize) { 1.0 / assetsSize }
    private val portfolios = Array(periods.size().toInt()) { initPortfolio() }

    private val random = GeometricDistribution(tradePeriodGeometricBias).apply {
        reseedRandomGenerator(657567L)
    }

    val size = periods.size() - batchSize

    suspend fun get(index: Long): TrainBatch {
        val indices = index until index + batchSize
        val batchPeriods = periods.slice(indices)
        val portfolio = portfolios.slice(indices.toInt()).map { it.toList() }
        fun setPortfolio(portfolio: PortionsBatch) {
            val portfolioArray = portfolio.map { it.toDoubleArray() }.toTypedArray()
            portfolios[indices.toInt()] = portfolioArray
        }
        val history = tradedHistories(archive, historyPeriods, tradeDelayPeriods, batchPeriods).toList()
        return TrainBatch(portfolio, ::setPortfolio, history, batchPeriods)
    }

    fun channel() = infiniteChannel {
        val lastIndex = size - 1
        val randomPeriod = lastIndex - random.limitSample(lastIndex.toInt())
        get(randomPeriod)
    }
}

class TrainBatch(
        val currentPortfolio: PortionsBatch,
        val setCurrentPortfolio: (PortionsBatch) -> Unit,
        val history: TradedHistoryBatch,
        val periods: PeriodProgression
)