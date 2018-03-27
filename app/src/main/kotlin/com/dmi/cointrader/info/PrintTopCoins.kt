package com.dmi.cointrader.info

import com.binance.api.client.domain.general.SymbolInfo
import com.dmi.cointrader.binance.api.BinanceAPI
import com.dmi.cointrader.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import java.time.Duration
import java.time.Instant

fun printTopCoins() = runBlocking {
    val beforeTime = Instant.now() - Duration.ofDays(20)
    val minVolume = 150
    val topCount = 70
    val excludedCoins = setOf("BNBBTC")

    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()

    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to it }

    val exist = info.keys.associate { it to existBefore(api, it, beforeTime) }
//    val volumesMonthBeforeTime = info.keys.associate { it to volume(api, it, 20 * 24, beforeTime) / 20 }
    val volumesMonth1 = info.keys.associate { it to volume(api, it, 20 * 24) / 20 }
//    val volumesMonth2 = info.keys.associate { it to volume(api, it, 20 * 24, Instant.now() - Duration.ofDays(20)) / 20 }
    val volumesWeek1 = info.keys.associate { it to volume(api, it, 7 * 24) / 7 }
    val volumesWeek2 = info.keys.associate { it to volume(api, it, 7 * 24, Instant.now() - Duration.ofDays(7)) / 7 }
    val volumesDay1 = info.keys.associate { it to volume(api, it, 1 * 24) / 1 }
    val volumesDay2 = info.keys.associate { it to volume(api, it, 1 * 24, Instant.now() - Duration.ofDays(1)) / 1 }
    val volumesDay3 = info.keys.associate { it to volume(api, it, 1 * 24, Instant.now() - Duration.ofDays(2)) / 1 }
//    val volumesMonthBeforeTimeList = volumesMonthBeforeTime.entries.filter { it.value >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesMonth1List = volumesMonth1.entries.filter { it.value >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
//    val volumesMonth2List = volumesMonth2.entries.filter { it.value >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesWeek1List = volumesWeek1.entries.filter { it.value >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesWeek2List = volumesWeek2.entries.filter { it.value >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesDay1List = volumesDay1.entries.filter { it.value >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesDay2List = volumesDay2.entries.filter { it.value >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
    val volumesDay3List = volumesDay3.entries.filter { it.value >= minVolume && exist[it.key] == true }.sortedByDescending { it.value }
//    val topCoinsMonthBeforeTime = volumesMonthBeforeTimeList.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsMonth1 = volumesMonth1List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
//    val topCoinsMonth2 = volumesMonth2List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsWeek1 = volumesWeek1List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsWeek2 = volumesWeek2List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsDay1 = volumesDay1List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsDay2 = volumesDay2List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoinsDay3 = volumesDay3List.map { it.key }.filter { !excludedCoins.contains(it) }.take(topCount)
    val topCoins = topCoinsMonth1 intersect topCoinsWeek1 intersect topCoinsWeek2 intersect topCoinsDay1 intersect topCoinsDay2 intersect topCoinsDay3

    val printList = listOf("USDT") + topCoins
    println(printList.joinToString(", ") { "\"$it\"" })
}

private suspend fun volume(client: BinanceAPI, coin: String, hourCount: Int, before: Instant? = null): Double {
    require(hourCount <= 500)
    return client.getCandlestickBars(coin, "1h", hourCount, null, before?.toEpochMilli()).map { it.quoteAssetVolume.toDouble() }.sum()
}

private suspend fun existBefore(client: BinanceAPI, coin: String, time: Instant): Boolean {
    return client.getCandlestickBars(coin, "1h", 100, null, time.toEpochMilli()).isNotEmpty()
}