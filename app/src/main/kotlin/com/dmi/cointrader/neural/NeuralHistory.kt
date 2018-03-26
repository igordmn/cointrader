package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.trade.TradeConfig
import com.dmi.util.collection.coerceIn
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel

typealias NeuralHistory = History
data class TradedHistory(val history: NeuralHistory, val tradeTimeSpreads: Spreads)
typealias TradedHistoryBatch = List<TradedHistory>

suspend fun tradedHistories(config: TradeConfig, archive: Archive, periods: PeriodProgression): ReceiveChannel<TradedHistory> {
    val bufferSize = 10000

}

suspend fun neuralHistory(config: TradeConfig, archive: Archive, period: Period): NeuralHistory {

    archive.historyAt()
}

fun PeriodRange.clampForTradedHistory(config: TradeConfig): PeriodRange {
    return coerceIn(config.historyPeriods * config.historySize - 1..endInclusive - config.tradeDelayPeriods)
}