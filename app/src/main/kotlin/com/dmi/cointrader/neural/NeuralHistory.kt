package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.trade.TradeConfig

typealias NeuralHistory = History
data class TradedHistory(val history: NeuralHistory, val tradeTimeSpreads: Spreads)
data class TradedHistoryBatch(val history: NeuralHistory, val tradeTimeSpreads: Spreads)

suspend fun tradedHistory(config: TradeConfig, archive: Archive, period: Period): TradedHistory {

}

suspend fun neuralHistory(config: TradeConfig, archive: Archive, period: Period): NeuralHistory {

}

fun PeriodRange.clampForTradedHistory(): PeriodRange {

}