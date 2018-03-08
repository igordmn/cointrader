package com.dmi.cointrader.main

import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.synchronizable
import com.dmi.util.concurrent.mapAsync
import com.dmi.util.io.SyncList
import old.exchange.binance.BinanceConstants
import old.exchange.binance.BinanceTime
import old.exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        val api = binanceAPI()
        val constants = BinanceConstants()
        val currentTime = BinanceTime(api).synchronizable()
        val config = Config()

        fun coinLog(coin: String) = object: SyncList.Log<Trade> {
            override fun itemsAppended(items: List<Trade>, indices: LongRange) {
                val lastTradeTime = items.last().time
                println("$coin $lastTradeTime")
            }
        }

        val coinToTrades = coinToCachedBinanceTrades(config, constants, api, currentTime, ::coinLog)
        val moments = cachedMoments(config, coinToTrades, currentTime)

        println("Download trades")
        currentTime.sync()
        coinToTrades.mapAsync {
            it.sync()
        }

        println("Make moments")
        moments.sync()
    }
}