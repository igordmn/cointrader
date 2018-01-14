package main

import com.binance.api.client.BinanceApiClientFactory
import exchange.binance.BinanceInfo
import exchange.binance.BinanceTime
import exchange.binance.market.BinanceMarketHistory
import exchange.binance.market.BinanceMarketPrices
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newAsyncRestClient()
    val info = BinanceInfo()
    val marketName = info.marketName("USDT", "BTC")!!
    val time = BinanceTime(client)
    val marketPrices = BinanceMarketPrices(marketName, client)
    val marketHistory = BinanceMarketHistory(marketName, client)

    runBlocking {
        val currentTime = time.current()
        println("Current time $currentTime")

        val price = marketPrices.current()
        println("Current price of $marketName: $price")
    }
}