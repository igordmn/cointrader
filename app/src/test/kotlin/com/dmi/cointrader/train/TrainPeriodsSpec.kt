package com.dmi.cointrader.train

import com.dmi.cointrader.archive.Spread
import com.dmi.cointrader.neural.TradedHistory
import com.dmi.cointrader.trade.HistoryPeriods
import com.dmi.util.collection.asSuspend
import com.dmi.util.concurrent.suspend
import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe

class TrainPeriodsSpec : Spec({
    "splitForTrain" {
        // 2L..30L step 5L == 2 7 12 17 22 27
        (2L..30L step 5L).splitForTrain(periodsPerDay = 1.5, testDays = 7.4, validationDays = 3.4) shouldBe TrainPeriods(
                train = 2L..22L step 5L,
                test = 17L..22L step 5L,
                validation = 27L..27L step 5L
        )
    }

    "TrainBatches" {
        val spreads00 = listOf(Spread(10.0, 2.0), Spread(30.0, 1.0))
        val spreads01 = listOf(Spread(20.0, 3.0), Spread(40.0, 2.0))
        val spreads02 = listOf(Spread(30.0, 4.0), Spread(50.0, 3.0))
        val spreads03 = listOf(Spread(40.0, 5.0), Spread(60.0, 4.0))
        val spreads04 = listOf(Spread(50.0, 6.0), Spread(70.0, 5.0))
        val spreads05 = listOf(Spread(60.0, 7.0), Spread(80.0, 6.0))
        val spreads06 = listOf(Spread(70.0, 8.0), Spread(90.0, 7.0))
        val spreads07 = listOf(Spread(80.0, 9.0), Spread(10.0, 8.0))
        val spreads08 = listOf(Spread(90.0, 1.0), Spread(20.0, 9.0))
        val spreads09 = listOf(Spread(900.0, 1.0), Spread(200.0, 9.0))
        val spreads10 = listOf(Spread(100.0, 2.0), Spread(300.0, 1.0))
        val spreads11 = listOf(Spread(200.0, 3.0), Spread(400.0, 2.0))
        val spreads12 = listOf(Spread(300.0, 4.0), Spread(500.0, 3.0))
        val spreads13 = listOf(Spread(400.0, 5.0), Spread(600.0, 4.0))
        val spreads14 = listOf(Spread(500.0, 6.0), Spread(700.0, 5.0))
        val spreads15 = listOf(Spread(600.0, 7.0), Spread(800.0, 6.0))
        val spreads16 = listOf(Spread(700.0, 8.0), Spread(900.0, 7.0))
        val spreads17 = listOf(Spread(800.0, 9.0), Spread(100.0, 8.0))
        val spreads18 = listOf(Spread(900.0, 1.0), Spread(200.0, 9.0))
        val spreads19 = listOf(Spread(900.0, 1.0), Spread(200.0, 9.0))
        val archive = listOf(
                spreads00, spreads01, spreads02, spreads03, spreads04, spreads05, spreads06, spreads07, spreads08, spreads09,
                spreads10, spreads11, spreads12, spreads13, spreads14, spreads15, spreads16, spreads17, spreads18, spreads19
        ).asSuspend()

        // 5L..18L step 3L == 5 8 11 14 17
        val batches = TrainBatches(
                archive,
                periods = 5L..18L step 3L, batchSize = 3, assetsSize = 2,
                historyPeriods = HistoryPeriods(count = 3, size = 2), tradeDelayPeriods = 1
        )

        val getBatches = suspend {
            (0 until batches.size).map { batches.get(it) }
        }

        val batchesList = getBatches()
        batchesList.map { it.history } shouldBe listOf(
                listOf(
                        TradedHistory(listOf(spreads01, spreads03, spreads05), spreads06),
                        TradedHistory(listOf(spreads04, spreads06, spreads08), spreads09),
                        TradedHistory(listOf(spreads07, spreads09, spreads11), spreads12)
                ),
                listOf(
                        TradedHistory(listOf(spreads04, spreads06, spreads08), spreads09),
                        TradedHistory(listOf(spreads07, spreads09, spreads11), spreads12),
                        TradedHistory(listOf(spreads10, spreads12, spreads14), spreads15)
                )
        )
        batchesList.map { it.currentPortfolio } shouldBe listOf(
                listOf(
                        listOf(0.5, 0.5),
                        listOf(0.5, 0.5),
                        listOf(0.5, 0.5)
                ),
                listOf(
                        listOf(0.5, 0.5),
                        listOf(0.5, 0.5),
                        listOf(0.5, 0.5)
                )
        )

        batchesList[0].setCurrentPortfolio(
                listOf(
                        listOf(0.2, 0.8),
                        listOf(0.9, 0.1),
                        listOf(0.3, 0.7)
                )
        )
        getBatches().map { it.currentPortfolio } shouldBe listOf(
                listOf(
                        listOf(0.2, 0.8),
                        listOf(0.9, 0.1),
                        listOf(0.3, 0.7)
                ),
                listOf(
                        listOf(0.9, 0.1),
                        listOf(0.3, 0.7),
                        listOf(0.5, 0.5)
                )
        )

        batchesList[1].setCurrentPortfolio(
                listOf(
                        listOf(0.2, 0.8),
                        listOf(0.9, 0.1),
                        listOf(0.3, 0.7)
                )
        )
        getBatches().map { it.currentPortfolio } shouldBe listOf(
                listOf(
                        listOf(0.2, 0.8),
                        listOf(0.2, 0.8),
                        listOf(0.9, 0.1)
                ),
                listOf(
                        listOf(0.2, 0.8),
                        listOf(0.9, 0.1),
                        listOf(0.3, 0.7)
                )
        )
    }
})