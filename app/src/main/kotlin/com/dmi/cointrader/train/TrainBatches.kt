package com.dmi.cointrader.train

import com.dmi.cointrader.archive.Archive
import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.neural.PortionsBatch
import com.dmi.cointrader.neural.TradedHistoryBatch
import com.dmi.cointrader.neural.tradedHistories
import com.dmi.cointrader.trade.TradeConfig
import com.dmi.util.collection.set
import com.dmi.util.collection.size
import com.dmi.util.collection.slice
import com.dmi.util.concurrent.infiniteChannel
import com.dmi.util.concurrent.suspend
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import java.util.*

fun trainBatches(
        trainPeriods: IntProgression,
        size: Int,
        config: TradeConfig,
        archive: Archive
): ReceiveChannel<TrainBatch> {
    fun initPortfolio(coinNumber: Int): DoubleArray = DoubleArray(coinNumber) { 1.0 / coinNumber }
    fun initPortfolios(size: Int, coinNumber: Int) = Array(size) { initPortfolio(coinNumber) }

    val portfolios = initPortfolios(trainPeriods.size(), config.assets.all.size).also {
        require(trainPeriods.first == 0)
    }

    val random = Random(867979346)

    val randomBatch = suspend {
        val startIndex = random.nextInt(trainPeriods.size() - size)
        val endIndex = startIndex + size
        val indices = startIndex until endIndex
        val batchPeriods = trainPeriods.slice(indices)
        val portfolio = portfolios.slice(indices).map { it.toList() }
        fun setPortfolio(portfolio: PortionsBatch) = portfolios.set(indices, portfolio.map { it.toDoubleArray() }.toTypedArray())
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