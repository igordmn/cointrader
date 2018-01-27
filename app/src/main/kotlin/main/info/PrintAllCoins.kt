package main.info

import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.Candlestick
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketLimits
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal
import util.math.sum

fun main(args: Array<String>) = runBlocking {
    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()

    val coins = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.map { it.symbol.removeSuffix("BTC") }
    print(coins)
}
