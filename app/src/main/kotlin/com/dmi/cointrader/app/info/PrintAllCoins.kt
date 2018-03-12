package com.dmi.cointrader.app.info

import com.dmi.cointrader.app.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) = runBlocking {
    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()

    val coins = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.map { it.symbol.removeSuffix("BTC") }
    print(coins)
}
