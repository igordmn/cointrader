package main

import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketBroker
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal

fun main(args: Array<String>) {
    System.setProperty("log.name", "buyTest")

    val apiKey = File("D:/binance/apiKey.txt").readText()
    val secret = File("D:/binance/secret.txt").readText()
    val log = LoggerFactory.getLogger("main")

    val api = binanceAPI(apiKey, secret, log)

    val broker = BinanceMarketBroker("LTCBNB", api, log)

    runBlocking {
        broker.sell(BigDecimal("0.00001"))
    }
}