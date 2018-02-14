package scratch

import exchange.binance.api.binanceAPI
import exchange.binance.BinanceConstants
import exchange.binance.BinancePortfolio
import kotlinx.coroutines.experimental.runBlocking
import java.io.File

fun main(args: Array<String>) {
    runBlocking {

        val apiKey = File("E:/Distr/Data/CryptoExchanges/binance/apiKey.txt").readText()
        val secret = File("E:/Distr/Data/CryptoExchanges/binance/secret.txt").readText()
        val api = binanceAPI(apiKey, secret)

        val constants = BinanceConstants()
        val binancePortfolio = BinancePortfolio(constants, api)
        println(binancePortfolio.amounts())
    }
}