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
    val all = clampForTradedHistory(tradeConfig.historyPeriods, tradeConfig.tradePeriods.delay).tradePeriods(tradeConfig.tradePeriods.size)
    val size = all.size()
    return TrainPeriods(
            train = all.slice(0 until size - validationSize),
            test = all.slice(size - testSize until size - validationSize),
            validation = all.slice(size - validationSize until size)
    )
}

data class TrainPeriods(val train: PeriodProgression, val test: PeriodProgression, val validation: PeriodProgression)

class TrainBatches(
        private val trainPeriods: PeriodProgression,
        private val batchSize: Int,
        private val config: TradeConfig,
        private val archive: SuspendList<Spreads>
) {
    private fun initPortfolio(coinNumber: Int): DoubleArray = DoubleArray(coinNumber) { 1.0 / coinNumber }
    private fun initPortfolios(size: Int, coinNumber: Int) = Array(size) { initPortfolio(coinNumber) }

    private val portfolios = initPortfolios(trainPeriods.size().toInt(), config.assets.all.size).also {
        require(trainPeriods.first == 0L)
    }

    private val random = Random(867979346)

    val size = trainPeriods.size() - batchSize

    suspend fun get(index: Long): TrainBatch {
        val indices = index until index + batchSize
        val batchPeriods = trainPeriods.slice(indices)
        val portfolio = portfolios.slice(indices.toInt()).map { it.toList() }
        fun setPortfolio(portfolio: PortionsBatch) = portfolios.set(indices.toInt(), portfolio.map { it.toDoubleArray() }.toTypedArray())
        val history = tradedHistories(archive, config.historyPeriods, config.tradePeriods.delay, batchPeriods).toList()
        return TrainBatch(portfolio, ::setPortfolio, history)
    }

    fun channel() = infiniteChannel { get(random.nextLong(size)) }
}

class TrainBatch(
        val currentPortfolio: PortionsBatch,
        val setCurrentPortfolio: (PortionsBatch) -> Unit,
        val history: TradedHistoryBatch
)