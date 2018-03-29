package com.dmi.cointrader.train

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.neural.PortionsBatch
import com.dmi.cointrader.neural.TradedHistoryBatch
import com.dmi.cointrader.neural.clampForTradedHistory
import com.dmi.cointrader.neural.tradedHistories
import com.dmi.cointrader.trade.TradeConfig
import com.dmi.util.collection.*
import com.dmi.util.concurrent.infiniteChannel
import com.dmi.util.concurrent.suspend
import com.dmi.util.math.nextLong
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import java.util.*

fun PeriodRange.splitForTrain(tradeConfig: TradeConfig, trainConfig: TrainConfig): TrainPeriods {
    val space = tradeConfig.periodSpace
    val testSize = (trainConfig.testDays * space.periodsPerDay()).toInt()
    val validationSize = (trainConfig.validationDays * space.periodsPerDay()).toInt()
    val all = clampForTradedHistory(tradeConfig).tradePeriods(tradeConfig.tradePeriods)
    val size = all.size()
    return TrainPeriods(
            train = all.slice(0 until size - validationSize),
            test = all.slice(size - testSize until size - validationSize),
            validation = all.slice(size - validationSize until size)
    )
}

data class TrainPeriods(val train: PeriodProgression, val test: PeriodProgression, val validation: PeriodProgression)

fun trainBatches(
        trainPeriods: PeriodProgression,
        size: Int,
        config: TradeConfig,
        archive: SuspendList<Spreads>
): ReceiveChannel<TrainBatch> {
    fun initPortfolio(coinNumber: Int): DoubleArray = DoubleArray(coinNumber) { 1.0 / coinNumber }
    fun initPortfolios(size: Int, coinNumber: Int) = Array(size) { initPortfolio(coinNumber) }

    val portfolios = initPortfolios(trainPeriods.size().toInt(), config.assets.all.size).also {
        require(trainPeriods.first == 0L)
    }

    val random = Random(867979346)

    val randomBatch = suspend {
        val startIndex = random.nextLong(trainPeriods.size() - size)
        val endIndex = startIndex + size
        val indices = startIndex until endIndex
        val batchPeriods = trainPeriods.slice(indices)
        val portfolio = portfolios.slice(indices.toInt()).map { it.toList() }
        fun setPortfolio(portfolio: PortionsBatch) = portfolios.set(indices.toInt(), portfolio.map { it.toDoubleArray() }.toTypedArray())
        val history = tradedHistories(config, archive, batchPeriods).toList()
        TrainBatch(portfolio, ::setPortfolio, history)
    }

    return infiniteChannel { randomBatch() }
}

class TrainBatch(
        val currentPortfolio: PortionsBatch,
        val setCurrentPortfolio: (PortionsBatch) -> Unit,
        val history: TradedHistoryBatch
)