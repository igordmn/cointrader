package com.dmi.cointrader.main

import com.dmi.cointrader.app.info.printTopCoins
import com.dmi.cointrader.app.performtrade.performRealTrades
import com.dmi.cointrader.app.test.backTest
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        when (args[0].toLowerCase()) {
            "train" -> train()
            "backtest" -> backTest(days = args[1].toDouble())
            "realtrades" -> performRealTrades()
            "topcoins" -> printTopCoins()
        }
    }
}