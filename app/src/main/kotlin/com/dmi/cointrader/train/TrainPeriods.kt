package com.dmi.cointrader.train

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.neural.PortionsBatch
import com.dmi.cointrader.neural.TradedHistoryBatch
import com.dmi.cointrader.neural.clampForTradedHistory
import com.dmi.cointrader.neural.tradedHistories
import com.dmi.cointrader.trade.HistoryPeriods
import com.dmi.cointrader.trade.TradeConfig
import com.dmi.util.collection.*
import com.dmi.util.concurrent.infiniteChannel
import com.dmi.util.math.nextLong
import kotlinx.coroutines.experimental.channels.toList
import java.util.*

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
            test = slice(size - validationSize - testSize until size - validationSize),
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
        tradeConfig.tradePeriods.delay
)

class TrainBatches(
        private val archive: SuspendList<Spreads>,
        private val periods: PeriodProgression,
        private val batchSize: Int,
        assetsSize: Int,
        private val historyPeriods: HistoryPeriods,
        private val tradeDelayPeriods: Int
) {
    private fun initPortfolio(coinNumber: Int): DoubleArray = DoubleArray(coinNumber) { 1.0 / coinNumber }
    private fun initPortfolios(size: Int, coinNumber: Int) = Array(size) { initPortfolio(coinNumber) }

    private val portfolios = initPortfolios(periods.size().toInt(), assetsSize).also {
        require(periods.first == 0L)
    }

    private val random = Random(867979346)

    val size = periods.size() - batchSize

    suspend fun get(index: Long): TrainBatch {
        val indices = index until index + batchSize
        val batchPeriods = periods.slice(indices)
        val portfolio = portfolios.slice(indices.toInt()).map { it.toList() }
        fun setPortfolio(portfolio: PortionsBatch) = portfolios.set(indices.toInt(), portfolio.map { it.toDoubleArray() }.toTypedArray())
        val history = tradedHistories(archive, historyPeriods, tradeDelayPeriods, batchPeriods).toList()
        return TrainBatch(portfolio, ::setPortfolio, history)
    }

    fun channel() = infiniteChannel { get(random.nextLong(size)) }
}

class TrainBatch(
        val currentPortfolio: PortionsBatch,
        val setCurrentPortfolio: (PortionsBatch) -> Unit,
        val history: TradedHistoryBatch
)