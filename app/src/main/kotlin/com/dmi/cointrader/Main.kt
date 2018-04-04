package com.dmi.cointrader

import com.dmi.cointrader.archive.downloadArchive
import com.dmi.cointrader.dl4j.dl4jtest
import com.dmi.cointrader.info.printTopCoins
import com.dmi.cointrader.trade.askAndPerformRealTrades
import com.dmi.cointrader.train.train
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        when (args[0].toLowerCase()) {
            "archive" -> downloadArchive()
            "train" -> train()
            "realtrades" -> askAndPerformRealTrades()
            "topcoins" -> printTopCoins()
            "dl4jtest" -> dl4jtest()
        }
    }
}