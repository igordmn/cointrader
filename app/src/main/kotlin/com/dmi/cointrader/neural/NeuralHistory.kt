package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.*
import com.dmi.cointrader.trade.HistoryPeriods
import com.dmi.cointrader.trade.TradeConfig
import com.dmi.util.collection.SuspendList
import com.dmi.util.collection.coerceIn
import com.dmi.util.concurrent.map
import com.dmi.util.concurrent.windowed
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.filterIndexed
import kotlinx.coroutines.experimental.channels.toList

typealias NeuralHistory = List<Spreads>

data class TradedHistory(val history: NeuralHistory, val tradeTimeSpreads: Spreads)
typealias TradedHistoryBatch = List<TradedHistory>

fun PeriodRange.clampForTradedHistory(config: HistoryPeriods, tradeDelayPeriods: Int): PeriodRange = with(config) {
    return coerceIn(size * count - 1..endInclusive - tradeDelayPeriods)
}

fun tradedHistories(
        config: HistoryPeriods,
        tradeDelayPeriods: Int,
        archive: SuspendList<Spreads>,
        periods: PeriodProgression
): ReceiveChannel<TradedHistory> = with(config) {
    archive
            .channel(periods.first - size * count + 1..periods.last + tradeDelayPeriods, bufferSize = 10000)
            .windowed(size * count + tradeDelayPeriods, periods.step.toInt())
            .map {
                val history = it.slice(0 until size * count).filterIndexed { i, _ ->
                    (i + 1) % size == 0
                }
                val tradeTimeSpreads = it.last()
                TradedHistory(history, tradeTimeSpreads)
            }
}

suspend fun neuralHistory(config: HistoryPeriods, archive: SuspendList<Spreads>, period: Period): NeuralHistory = with(config) {
    return archive
            .channel(period - count * size + 1..period, bufferSize = 10000)
            .filterIndexed { i, _ ->
                (i + 1) % size == 0
            }
            .toList()
}