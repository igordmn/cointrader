package main.info

import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.Candlestick
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketLimits
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal

fun main(args: Array<String>) = runBlocking {
    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo.await()
    val allPrices = api.latestPrices.await()

    val oneBTCinUSDT = BigDecimal(allPrices.find { it.symbol == "BTCUSDT" }!!.price)
    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to it }
    val prices: Map<String, BigDecimal> = allPrices.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to BigDecimal(it.price) }
    val allLimits: Map<String, BinanceMarketLimits> = prices.keys.associate { it to BinanceMarketLimits(it, exchangeInfo) }
    val volumes = info.keys.associate { it to BigDecimal(lastCandle(api, it).quoteAssetVolume) }
    val volumesList = volumes.entries.sortedByDescending { it.value }
    val topCoins = volumesList.map { it.key }.take(50)

    val infos = topCoins.map {
        val limits = allLimits[it]!!.get()
        val price = prices[it]!!
        val volume = volumes[it]!!
        it to CoinInfo(volume * oneBTCinUSDT / BigDecimal(30), limits.amountStep * price * oneBTCinUSDT, limits.minTotalPrice * oneBTCinUSDT)
    }
    infos.forEach(::println)
}


private suspend fun lastCandle(client: BinanceAPI, coin: String): Candlestick {
    return client.getCandlestickBars(coin, "1M", 1, null, null).await().last()
}

// Prices in USDT
private data class CoinInfo(val volume: BigDecimal, val amountStep: BigDecimal, val minTotalPrice: BigDecimal)