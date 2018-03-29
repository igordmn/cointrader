package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.trade.TradeConfig
import com.dmi.util.collection.SuspendList
import com.dmi.util.collection.coerceIn
import com.dmi.util.collection.chunked
import com.dmi.util.concurrent.map
import com.dmi.util.concurrent.windowed
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.filterIndexed
import kotlinx.coroutines.experimental.channels.toList

typealias NeuralHistory = List<Spreads>

data class TradedHistory(val history: NeuralHistory, val tradeTimeSpreads: Spreads)
typealias TradedHistoryBatch = List<TradedHistory>

fun PeriodRange.clampForTradedHistory(config: TradeConfig): PeriodRange = with(config) {
    return coerceIn(historyPeriods * historySize - 1..endInclusive - tradeDelayPeriods)
}

fun tradedHistories(
        config: TradeConfig,
        archive: Archive,
        periods: PeriodProgression
): ReceiveChannel<TradedHistory> = with(config) {
    archive
            .historyAt(periods.first - historyPeriods * historySize + 1..periods.last + tradeDelayPeriods)
            .windowed(historyPeriods * historySize + tradeDelayPeriods, periods.step)
            .map {
                val history = it.slice(0 until historyPeriods * historySize).filterIndexed { i, _ ->
                    (i + 1) % historyPeriods == 0
                }
                val tradeTimeSpreads = it.last()
                TradedHistory(history, tradeTimeSpreads)
            }
}

suspend fun neuralHistory(config: TradeConfig, archive: Archive, period: Period): NeuralHistory = with(config) {
    return archive
            .historyAt(period - historySize * historyPeriods + 1..period)
            .filterIndexed { i, _ ->
                (i + 1) % historyPeriods == 0
            }
            .toList()
}