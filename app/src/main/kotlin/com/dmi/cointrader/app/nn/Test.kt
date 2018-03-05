package com.dmi.cointrader.app.nn

import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.atom.ReadAtom
import com.dmi.util.atom.synchronizable
import exchange.binance.BinanceConstants
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import main.test.Config
import java.time.Instant


fun main(args: Array<String>) {
    runBlocking {
        val api = binanceAPI()
        val constants = BinanceConstants()
        val config = Config()
        val currentTime = object : ReadAtom<Instant> {
            suspend override fun invoke(): Instant = Instant.ofEpochMilli(api.serverTime().serverTime)
        }.synchronizable()

        val coinToTrades = coinToCachedBinanceTrades(config, constants, api, currentTime)
        val moments = cachedMoments(config, coinToTrades, currentTime)
        currentTime.sync()
        coinToTrades.forEach { it.sync() }
        moments.sync()
    }
}