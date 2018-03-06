package main

import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.atom.synchronizable
import exchange.binance.BinanceConstants
import exchange.binance.BinanceTime
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import main.test.Config

fun main(args: Array<String>) {
    runBlocking {
        val api = binanceAPI()
        val constants = BinanceConstants()
        val currentTime = BinanceTime(api).synchronizable()
        val config = Config()

        val coinToTrades = coinToCachedBinanceTrades(config, constants, api, currentTime)
        val moments = cachedMoments(config, coinToTrades, currentTime)
    }
}