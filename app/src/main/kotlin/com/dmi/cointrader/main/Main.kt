package com.dmi.cointrader.main

import com.dmi.cointrader.app.info.printTopCoins
import com.dmi.cointrader.app.performtrade.performRealTrades
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        when (args[0].toLowerCase()) {
            "realtrades" -> performRealTrades()
            "topcoins" -> printTopCoins()
        }
    }
}