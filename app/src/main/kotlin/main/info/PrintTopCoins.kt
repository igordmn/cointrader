package main.info

import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.Candlestick
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketLimits
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal

fun printTopCoins() = runBlocking {
    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()
    val allPrices = api.allPrices()

    val oneBTCinUSDT = BigDecimal(allPrices.find { it.symbol == "BTCUSDT" }!!.price)
    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to it }
    val prices: Map<String, BigDecimal> = allPrices.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to BigDecimal(it.price) }
    val allLimits: Map<String, BinanceMarketLimits> = prices.keys.associate { it to BinanceMarketLimits(it, exchangeInfo) }
    val volumesMonth = info.keys.associate { it to BigDecimal(lastCandle(api, it, "1M").quoteAssetVolume) / BigDecimal(30) }
    val volumesWeek = info.keys.associate { it to BigDecimal(lastCandle(api, it, "1w").quoteAssetVolume) / BigDecimal(7) }
    val volumesDay = info.keys.associate { it to BigDecimal(lastCandle(api, it, "1d").quoteAssetVolume) }
    val volumesMonthList = volumesMonth.entries.sortedByDescending { it.value }
    val volumesWeekList = volumesWeek.entries.sortedByDescending { it.value }
    val volumesDayList = volumesDay.entries.sortedByDescending { it.value }
    val topCoinsMonth = volumesMonthList.map { it.key }.take(70)
    val topCoinsWeek = volumesWeekList.map { it.key }.take(70)
    val topCoinsDay = volumesDayList.map { it.key }.take(70)

    val topCoins = topCoinsMonth - (topCoinsMonth - topCoinsWeek)

    val infos = topCoins.map {
        val limits = allLimits[it]!!.get()
        val price = prices[it]!!
        val volumeMonth = volumesMonth[it]!!
        val volumeWeek = volumesWeek[it]!!
        val volumeDay = volumesDay[it]!!
        it to CoinInfo(volumeMonth * oneBTCinUSDT, volumeWeek * oneBTCinUSDT, volumeDay * oneBTCinUSDT, limits.amountStep * price * oneBTCinUSDT)
    }
    .filter {
        it.second.amountStep <= BigDecimal(2)
    }
    infos.forEach(::println)
    println(infos.joinToString(", ") { it.first })
}


private suspend fun lastCandle(client: BinanceAPI, coin: String, period: String): Candlestick {
    return client.getCandlestickBars(coin, period, 1, null, null).last()
}

// Prices in USDT
private data class CoinInfo(val volumeMonth: BigDecimal, val volumeWeek: BigDecimal, val volumeDay: BigDecimal, val amountStep: BigDecimal) {
    override fun toString(): String {
        return "$volumeMonth\t$volumeWeek\t$amountStep"
    }
}