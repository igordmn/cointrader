package com.dmi.cointrader.neural

import com.dmi.cointrader.archive.Spread
import com.dmi.util.collection.asSuspend
import com.dmi.util.test.Spec

class NeuralHistorySpec: Spec({
    val spreads1 = listOf(Spread(10.0, 2.0), Spread(30.0, 1.0))
    val spreads2 = listOf(Spread(20.0, 3.0), Spread(40.0, 2.0))
    val spreads3 = listOf(Spread(30.0, 4.0), Spread(50.0, 3.0))
    val spreads4 = listOf(Spread(40.0, 5.0), Spread(60.0, 4.0))
    val spreads5 = listOf(Spread(50.0, 6.0), Spread(70.0, 5.0))
    val spreads6 = listOf(Spread(60.0, 7.0), Spread(80.0, 6.0))
    val archive = listOf(spreads1, spreads2, spreads3, spreads4, spreads5, spreads6).asSuspend()

    "clampForTradedHistory" {

    }

    "tradedHistories" {

    }
})