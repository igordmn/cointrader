package com.dmi.cointrader.app.info

import com.binance.api.client.domain.general.SymbolInfo
import com.dmi.cointrader.app.binance.BinanceConstants
import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.cointrader.app.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal
import com.dmi.util.math.sum
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun printTopCoins() = runBlocking {
    val beforeTime = LocalDateTime.of(2018, 2, 15, 0, 0, 0, 0).toInstant(ZoneOffset.ofHours(3))
    val minVolume = BigDecimal(180)
    val topCount = 70
    val excludedCoins = setOf("BNBBTC")

    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()

    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to it }

    val exist = info.keys.associate { it to existBefore(api, it, beforeTime) }
//    val volumesMonthBeforeTime = info.keys.associate { it to volume(api, it, 20 * 24, beforeTime) / BigDecimal(20) }
    val volumesMonth1 = info.keys.associate { it to volume(api, it, 20 * 24) / BigDecimal(20) }
//    val volumesMonth2 = info.keys.associate { it to volume(api, it, 20 * 24, Instant.now() - Duration.ofDays(20)) / BigDecimal(20) }
    val volumesWeek1 = info.keys.associate { it to volume(api, it, 7 * 24) / BigDecimal(7) }
    val volumesWeek2 = info.keys.associate { it to volume(api, it, 7 * 24, Instant.now() - Duration.ofDays(7)) / BigDecimal(7) }
    val volumesDay1 = info.keys.associate { it to volume(api, it, 1 * 24) / BigDecimal(1) }
    val volumesDay2 = info.keys.associate { it to volume(api, it, 1 * 24, Instant.now() - Duration.ofDays(1)) / BigDecimal(1) }
    val volumesDay3 = info.keys.associate { it to volume(api, it, 1 * 24, Instant.now() - Duration.ofDays(2)) / BigDecimal(1) }
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
//    val topCoins = topCoinsMonth1 intersect topCoinsMonth2 intersect topCoinsWeek1 intersect topCoinsWeek2 intersect topCoinsDay1 intersect topCoinsDay2 intersect topCoinsDay3
    val topCoins = topCoinsMonth1 intersect topCoinsWeek1 intersect topCoinsWeek2 intersect topCoinsDay1 intersect topCoinsDay2 intersect topCoinsDay3

    val infos = topCoins
            .map {
//                val volumeMonthBeforeTime = volumesMonthBeforeTime[it]!!
//                val volumeMonth1 = volumesMonth1[it]!!
//                val volumeMonth2 = volumesMonth2[it]!!
                val volumeWeek1 = volumesWeek1[it]!!
                val volumeWeek2 = volumesWeek2[it]!!
                val volumeMonthBeforeTime = BigDecimal.ZERO
                val volumeMonth1 = BigDecimal.ZERO
                val volumeMonth2 = BigDecimal.ZERO
                val volumeDay1 = volumesDay1[it]!!
                val volumeDay2 = volumesDay2[it]!!
                val volumeDay3 = volumesDay3[it]!!

                val name = it.removeSuffix("BTC")
                CoinInfo(
                        name,
                        volumeMonthBeforeTime,
                        volumeMonth1,
                        volumeMonth2,
                        volumeWeek1,
                        volumeWeek2,
                        volumeDay1,
                        volumeDay2,
                        volumeDay3
                )
            }

    val printList = listOf("USDT") + infos.map { it.name }
    println(printList.joinToString(", ") { "\"$it\"" })
}

private suspend fun volume(client: BinanceAPI, coin: String, hourCount: Int, before: Instant? = null): BigDecimal {
    require(hourCount <= 500)
    return client.getCandlestickBars(coin, "1h", hourCount, null, before?.toEpochMilli()).map { BigDecimal(it.quoteAssetVolume) }.sum()
}

private suspend fun existBefore(client: BinanceAPI, coin: String, time: Instant): Boolean {
    return client.getCandlestickBars(coin, "1h", 100, null, time.toEpochMilli()).isNotEmpty()
}

private data class CoinInfo(
        val name: String,
        val volumeMonthBeforeTime: BigDecimal,
        val volumeMonth1: BigDecimal,
        val volumeMonth2: BigDecimal,
        val volumeWeek1: BigDecimal,
        val volumeWeek2: BigDecimal,
        val volumeDay1: BigDecimal,
        val volumeDay2: BigDecimal,
        val volumeDay3: BigDecimal
) {
    override fun toString(): String {
        return "$name\t$volumeMonthBeforeTime\t$volumeMonth1\t$volumeMonth2\t$volumeWeek1\t$volumeWeek2\t$volumeDay1\t$volumeDay2\t$volumeDay3"
    }
}