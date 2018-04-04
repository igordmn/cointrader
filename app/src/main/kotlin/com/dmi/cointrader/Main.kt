package com.dmi.cointrader

import com.dmi.cointrader.archive.downloadArchive
import com.dmi.cointrader.dl4j.simpleToy
import com.dmi.cointrader.info.printTopCoins
import com.dmi.cointrader.trade.askAndPerformRealTrades
import com.dmi.cointrader.trade.performRealTrades
import com.dmi.cointrader.train.train
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        when (args[0].toLowerCase()) {
            "archive" -> downloadArchive()
            "train" -> train()
            "realtrades" -> askAndPerformRealTrades()
            "topcoins" -> printTopCoins()
            "dl4jtest" -> simpleToy()
        }
    }
}