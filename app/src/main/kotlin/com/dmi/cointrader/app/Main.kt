package com.dmi.cointrader.app

import com.dmi.cointrader.app.info.printTopCoins
import com.dmi.cointrader.app.trade.performRealTrades
import com.dmi.cointrader.app.train.train
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        when (args[0].toLowerCase()) {
            "train" -> train()
            "realtrades" -> performRealTrades()
            "topcoins" -> printTopCoins()
        }
    }
}