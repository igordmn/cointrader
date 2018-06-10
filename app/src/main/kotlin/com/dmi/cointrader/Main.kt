package com.dmi.cointrader

import com.dmi.cointrader.archive.downloadArchive
import com.dmi.cointrader.info.printTopCoins
import com.dmi.cointrader.trade.backtest
import com.dmi.cointrader.trade.backtestBest
import com.dmi.cointrader.trade.performRealTrades
import com.dmi.cointrader.train.showBestNets
import com.dmi.cointrader.train.train
import com.dmi.cointrader.train.trainMulti
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        when (args[0].toLowerCase()) {
            "archive" -> downloadArchive()
            "train" -> train()
            "trainmulti" -> trainMulti()
            "backtest" -> backtest(daysList = args.drop(1).map { it.toDouble() })
            "backtestbest" -> backtestBest(daysList = args.drop(1).map { it.toDouble() })
            "realtrades" -> performRealTrades()
            "topcoins" -> printTopCoins()
            "bestnets" -> showBestNets(count = args[1].toInt())
        }
    }
}