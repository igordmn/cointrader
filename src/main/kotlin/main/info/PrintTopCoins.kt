package main.info

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import exchange.binance.market.BinanceMarketLimits
import java.math.BigDecimal

fun main(args: Array<String>) {
    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newRestClient()
    val exchangeInfo = client.exchangeInfo
    val allPrices = client.allPrices

    val oneBTCinUSDT = BigDecimal(allPrices.find { it.symbol == "BTCUSDT" }!!.price)
    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to it }
    val prices: Map<String, BigDecimal> = allPrices.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to BigDecimal(it.price) }
    val allLimits: Map<String, BinanceMarketLimits> = prices.keys.associate { it to BinanceMarketLimits(it, exchangeInfo) }
    val volumes = info.keys.associate { it to BigDecimal(lastCandle(client, it).quoteAssetVolume) }
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


private fun lastCandle(client: BinanceApiRestClient, coin: String): Candlestick {
    return client.getCandlestickBars(coin, CandlestickInterval.MONTHLY, 1, null, null).last()
}

// Prices in USDT
private data class CoinInfo(val volume: BigDecimal, val amountStep: BigDecimal, val minTotalPrice: BigDecimal)