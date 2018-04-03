package com.dmi.cointrader.info

import com.binance.api.client.domain.general.SymbolInfo
import com.dmi.cointrader.binance.api.BinanceAPI
import com.dmi.cointrader.binance.api.binanceAPI
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.map
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking
import java.time.Instant

fun printTopCoins() = runBlocking {
    data class CoinVolumes(val coin: String, val daylyVolumes: List<Double>)

    val days = 30
    val minDayVolume = 150
    val averageByDays = 3
    val excludedPairs = setOf("BNBBTC", "CTRBTC")

    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()
    val time = Instant.ofEpochMilli(api.serverTime().serverTime)
    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith("BTC") }.associate { it.symbol to it }

    val allPairs = info.keys
    val coinVolumes = allPairs
            .filter { !excludedPairs.contains(it) }
            .map {
                CoinVolumes(
                        coin = it.removeSuffix("BTC"),
                        daylyVolumes = daylyVolumes(api, it, time).take(days).toList()
                )
            }

    val coins = coinVolumes
            .filter {
                val averageVolumes = it.daylyVolumes.windowed(averageByDays) { it.average() }
                averageVolumes.any { it > minDayVolume }
            }
            .map { it.coin }

    val printList = listOf("USDT") + coins
    println(printList.joinToString(", ") { "\"$it\"" })
}

private suspend fun daylyVolumes(client: BinanceAPI, coin: String, before: Instant): ReceiveChannel<Double> {
    return hourlyVolumes(client, coin, before).chunked(24).map { it.sum() }
}

private suspend fun hourlyVolumes(client: BinanceAPI, coin: String, before: Instant): ReceiveChannel<Double> = produce {
    var t = before.toEpochMilli()
    while (true) {
        val values = client.getCandlestickBars(coin, "1h", 500, null, t)
        values.map { it.quoteAssetVolume.toDouble() }.reversed().forEach {
            send(it)
        }
        if (values.isEmpty()) {
            break
        }
        t = values.first().openTime - 1
    }
}