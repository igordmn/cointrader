package main.info

import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.Candlestick
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketLimits
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal
import util.math.sum
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// todo https://api.coinmarketcap.com/v1/ticker/?limit=100
fun printTopCoins() = runBlocking {
    val beforeTime = LocalDateTime.of(2018, 1, 1, 0, 0, 0, 0).toInstant(ZoneOffset.ofHours(3))

    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()
    val allPrices = api.allPrices()

    val oneBTCinUSDT = BigDecimal(allPrices.find { it.symbol == "BTCUSDT" }!!.price)
    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to it }
    val prices: Map<String, BigDecimal> = allPrices.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to BigDecimal(it.price) }
    val allLimits: Map<String, BinanceMarketLimits> = prices.keys.associate { it to BinanceMarketLimits(it, exchangeInfo) }

    val exist = info.keys.associate { it to existBefore(api, it, beforeTime) }
    val volumesMonth = info.keys.associate { it to volume(api, it, 20 * 24) / BigDecimal(20) }
    val volumesWeek = info.keys.associate { it to volume(api, it, 7 * 24) / BigDecimal(7) }
    val volumesDay = info.keys.associate { it to volume(api, it, 1 * 24) / BigDecimal(1) }
    val volumesMonthList = volumesMonth.entries.sortedByDescending { it.value }
    val volumesWeekList = volumesWeek.entries.sortedByDescending { it.value }
    val volumesDayList = volumesDay.entries.sortedByDescending { it.value }
    val topCoinsMonth = volumesMonthList.map { it.key }.take(70)
    val topCoinsWeek = volumesWeekList.map { it.key }.take(70)
    val topCoinsDay = volumesDayList.map { it.key }.take(70)

    val topCoins = (topCoinsMonth - (topCoinsMonth - topCoinsWeek) - (topCoinsMonth - topCoinsDay)).filter { exist[it] == true }

    val infos = topCoins.map {
        val limits = allLimits[it]!!.get()
        val price = prices[it]!!
        val volumeMonth = volumesMonth[it]!!
        val volumeWeek = volumesWeek[it]!!
        val volumeDay = volumesDay[it]!!
        CoinInfo(it, volumeMonth * oneBTCinUSDT, volumeWeek * oneBTCinUSDT, volumeDay * oneBTCinUSDT, limits.amountStep * price * oneBTCinUSDT)
    }
    infos.forEach(::println)
}


private suspend fun volume(client: BinanceAPI, coin: String, hourCount: Int): BigDecimal {
    require(hourCount <= 500)
    return client.getCandlestickBars(coin, "1h", hourCount, null, null).map { BigDecimal(it.quoteAssetVolume) }.sum()
}

private suspend fun existBefore(client: BinanceAPI, coin: String, time: Instant): Boolean {
    return client.getCandlestickBars(coin, "1h", 100, null, time.toEpochMilli()).isNotEmpty()
}

// Prices in USDT
private data class CoinInfo(val name: String, val volumeMonth: BigDecimal, val volumeWeek: BigDecimal, val volumeDay: BigDecimal, val amountStep: BigDecimal) {
    override fun toString(): String {
        return "$name\t$volumeMonth\t$volumeWeek\t$volumeDay\t$amountStep"
    }
}