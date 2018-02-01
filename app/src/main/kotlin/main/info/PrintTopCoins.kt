package main.info

import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.Candlestick
import exchange.binance.BinanceConstants
import exchange.binance.api.BinanceAPI
import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketLimits
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal
import util.math.sum
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// todo https://api.coinmarketcap.com/v1/ticker/?limit=100
fun printTopCoins() = runBlocking {
    val beforeTime = LocalDateTime.of(2018, 1, 1, 0, 0, 0, 0).toInstant(ZoneOffset.ofHours(3))
    val minVolume = BigDecimal(2_000_000)
    val topCount = 70
    val excludedCoins = setOf("BNBBTC")

    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()
    val allPrices = api.allPrices()
    val constants = BinanceConstants()

    val oneBTCinUSDT = BigDecimal(allPrices.find { it.symbol == "BTCUSDT" }!!.price)
    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to it }
    val prices: Map<String, BigDecimal> = allPrices.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to BigDecimal(it.price) }
    val allLimits: Map<String, BinanceMarketLimits> = prices.keys.associate { it to BinanceMarketLimits(it, exchangeInfo) }

    val exist = info.keys.associate { it to existBefore(api, it, beforeTime) }
    val volumesMonth = info.keys.associate { it to volume(api, it, 20 * 24) / BigDecimal(20) }
    val volumesWeek = info.keys.associate { it to volume(api, it, 7 * 24) / BigDecimal(7) }
    val volumesDay1 = info.keys.associate { it to volume(api, it, 1 * 24) / BigDecimal(1) }
    val volumesDay2 = info.keys.associate { it to volume(api, it, 1 * 24, Instant.now() - Duration.ofDays(1)) / BigDecimal(1) }
    val volumesDay3 = info.keys.associate { it to volume(api, it, 1 * 24, Instant.now() - Duration.ofDays(2)) / BigDecimal(1) }
    val volumesMonthList = volumesMonth.entries.filter { it.value * oneBTCinUSDT >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesWeekList = volumesWeek.entries.filter { it.value * oneBTCinUSDT >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesDay1List = volumesDay1.entries.filter { it.value * oneBTCinUSDT >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesDay2List = volumesDay2.entries.filter { it.value * oneBTCinUSDT >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesDay3List = volumesDay3.entries.filter { it.value * oneBTCinUSDT >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val topCoinsMonth = volumesMonthList.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsWeek = volumesWeekList.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsDay1 = volumesDay1List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsDay2 = volumesDay2List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsDay3 = volumesDay3List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoins = topCoinsMonth intersect topCoinsWeek intersect topCoinsDay1 intersect topCoinsDay2 intersect topCoinsDay3

    val infos = topCoins
            .map {
                val limits = allLimits[it]!!.get()
                val price = prices[it]!!
                val volumeMonth = volumesMonth[it]!!
                val volumeWeek = volumesWeek[it]!!
                val volumeDay1 = volumesDay1[it]!!
                val volumeDay2 = volumesDay2[it]!!
                val volumeDay3 = volumesDay3[it]!!

                val name = it.removeSuffix("BTC")
                CoinInfo(
                        constants.binanceNameToStandard[name] ?: name,
                        volumeMonth * oneBTCinUSDT,
                        volumeWeek * oneBTCinUSDT,
                        volumeDay1 * oneBTCinUSDT,
                        volumeDay2 * oneBTCinUSDT,
                        volumeDay3 * oneBTCinUSDT,
                        limits.amountStep * price * oneBTCinUSDT
                )
            }
    infos.forEach(::println)
    println(infos.map { it.name })
}

private suspend fun volume(client: BinanceAPI, coin: String, hourCount: Int, before: Instant? = null): BigDecimal {
    require(hourCount <= 500)
    return client.getCandlestickBars(coin, "1h", hourCount, null, before?.toEpochMilli()).map { BigDecimal(it.quoteAssetVolume) }.sum()
}

private suspend fun existBefore(client: BinanceAPI, coin: String, time: Instant): Boolean {
    return client.getCandlestickBars(coin, "1h", 100, null, time.toEpochMilli()).isNotEmpty()
}

// Prices in USDT
private data class CoinInfo(
        val name: String,
        val volumeMonth: BigDecimal,
        val volumeWeek: BigDecimal,
        val volumeDay1: BigDecimal,
        val volumeDay2: BigDecimal,
        val volumeDay3: BigDecimal,
        val amountStep: BigDecimal
) {
    override fun toString(): String {
        return "$name\t$volumeMonth\t$volumeWeek\t$volumeDay1\t$volumeDay2\t$volumeDay3\t$amountStep"
    }
}