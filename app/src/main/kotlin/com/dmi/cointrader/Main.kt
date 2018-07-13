package com.dmi.cointrader

import com.dmi.cointrader.archive.downloadArchive
import com.dmi.cointrader.info.printTopCoins
import com.dmi.cointrader.trade.backtest
import com.dmi.cointrader.trade.backtestBest
import com.dmi.cointrader.trade.performRealTrades
import com.dmi.cointrader.train.showBestNets
import com.dmi.cointrader.train.train
import com.dmi.cointrader.train.trainMulti
import com.dmi.util.lang.unsupported
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        when (args[0].toLowerCase()) {
            "archive" -> downloadArchive()
            "train" -> train()
            "trainmulti" -> trainMulti()
            "backtest" -> backtest(maxDays = args[1].toDouble(), fee = if (args.size >= 3) args[2].toDouble() else null)
            "backtestbest" -> backtestBest(maxDays= args[1].toDouble(), fee = if (args.size >= 3) args[2].toDouble() else null)
            "realtrades" -> performRealTrades()
            "topcoins" -> printTopCoins()
            "bestnets" -> showBestNets(count = args[1].toInt())
            else -> unsupported()
        }
    }
}