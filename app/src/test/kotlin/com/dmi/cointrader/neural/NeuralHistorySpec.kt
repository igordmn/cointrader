package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.Spread
import com.dmi.cointrader.HistoryPeriods
import com.dmi.util.collection.asSuspend
import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.toList

class NeuralHistorySpec: Spec({
    val spreads0 = listOf(Spread(10.0, 2.0), Spread(30.0, 1.0))
    val spreads1 = listOf(Spread(20.0, 3.0), Spread(40.0, 2.0))
    val spreads2 = listOf(Spread(30.0, 4.0), Spread(50.0, 3.0))
    val spreads3 = listOf(Spread(40.0, 5.0), Spread(60.0, 4.0))
    val spreads4 = listOf(Spread(50.0, 6.0), Spread(70.0, 5.0))
    val spreads5 = listOf(Spread(60.0, 7.0), Spread(80.0, 6.0))
    val spreads6 = listOf(Spread(70.0, 8.0), Spread(90.0, 7.0))
    val spreads7 = listOf(Spread(80.0, 9.0), Spread(10.0, 8.0))
    val spreads8 = listOf(Spread(90.0, 1.0), Spread(20.0, 9.0))
    val archive = listOf(spreads0, spreads1, spreads2, spreads3, spreads4, spreads5, spreads6, spreads7, spreads8).asSuspend()

    val historyPeriods = HistoryPeriods(count = 2, size = 3)

    "clampForTradedHistory1" {
        (1L..20L).clampForTradedHistory(historyPeriods, 1) shouldBe (6L..19L)
        (0L..8L).clampForTradedHistory(historyPeriods, 1) shouldBe (5L..7L)
    }

    "clampForTradedHistory2" {
        (1L..20L).clampForTradedHistory(historyPeriods, 0) shouldBe (6L..20L)
        (0L..8L).clampForTradedHistory(historyPeriods, 0) shouldBe (5L..8L)
    }

    "tradedHistories1" {
        tradedHistories(archive, historyPeriods, 1, 5L..7L).toList() shouldBe listOf(
                TradedHistory(listOf(spreads2, spreads5), spreads6),
                TradedHistory(listOf(spreads3, spreads6), spreads7),
                TradedHistory(listOf(spreads4, spreads7), spreads8)
        )
        tradedHistories(archive, historyPeriods, 1, 5L..7L step 2).toList() shouldBe listOf(
                TradedHistory(listOf(spreads2, spreads5), spreads6),
                TradedHistory(listOf(spreads4, spreads7), spreads8)
        )
    }

    "tradedHistories2" {
        tradedHistories(archive, historyPeriods, 0, 5L..8L).toList() shouldBe listOf(
                TradedHistory(listOf(spreads2, spreads5), spreads5),
                TradedHistory(listOf(spreads3, spreads6), spreads6),
                TradedHistory(listOf(spreads4, spreads7), spreads7),
                TradedHistory(listOf(spreads5, spreads8), spreads8)
        )
        tradedHistories(archive, historyPeriods, 0, 5L..8L step 2).toList() shouldBe listOf(
                TradedHistory(listOf(spreads2, spreads5), spreads5),
                TradedHistory(listOf(spreads4, spreads7), spreads7)
        )
    }

    "neuralHistory" {
        neuralHistory(archive, historyPeriods, 5L) shouldBe listOf(spreads2, spreads5)
        neuralHistory(archive, historyPeriods, 6L) shouldBe listOf(spreads3, spreads6)
        neuralHistory(archive, historyPeriods, 8L) shouldBe listOf(spreads5, spreads8)
    }
})