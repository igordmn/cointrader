package com.dmi.cointrader.app.nn

import com.dmi.cointrader.app.moment.moments
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
        val currentTime = Instant.ofEpochMilli(api.serverTime().serverTime)
        val moments = moments(config, api, constants, currentTime)
    }
}