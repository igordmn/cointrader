package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.trade.TradeConfig
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel

typealias NeuralHistory = History
typealias NeuralHistoryBatch = HistoryBatch
data class TradedHistory(val history: NeuralHistory, val tradeTimeSpreads: Spreads)
data class TradedHistoryBatch(val history: NeuralHistory, val tradeTimeSpreads: Spreads)

suspend fun tradedHistory(config: TradeConfig, archive: Archive, period: Period): TradedHistory {

}
suspend fun tradedHistories(config: TradeConfig, archive: Archive, range: PeriodRange): ReceiveChannel<TradedHistory> {
    range.tradePeriods(config.tradePeriods).asReceiveChannel()
}

suspend fun tradedHistoryBatch(config: TradeConfig, archive: Archive, period: Period): TradedHistoryBatch {

}

suspend fun neuralHistory(config: TradeConfig, archive: Archive, period: Period): NeuralHistory {

}

fun PeriodRange.clampForTradedHistoryBatch(): PeriodRange {

}