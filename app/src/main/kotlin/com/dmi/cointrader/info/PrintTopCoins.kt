package com.dmi.cointrader.info

import com.binance.api.client.domain.general.SymbolInfo
import com.dmi.cointrader.binance.api.BinanceAPI
import com.dmi.cointrader.binance.api.binanceAPI
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.map
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking

fun printTopCoins() = runBlocking {
    data class CoinVolumes(val coin: String, val dailyVolumes: List<Double>)

    val days = 52
    val minDayVolume = 109   // in BTC
    val maxByDays = 3
    val main ="BTC"
    val excludedPairs = setOf(
            "BNBBTC", "BNBETH", // BNB for fees
            "CTRBTC", "CTRETH", // scam ICO
            "TUSDBTC", "TUSDETH", "TUSDUSDT" // BTCUSDT is better, because of higher volume
    )

    val api = binanceAPI()
    val exchangeInfo = api.exchangeInfo()
    val time = api.serverTime().serverTime
    val info: Map<String, SymbolInfo> = exchangeInfo.symbols.filter { it.symbol.endsWith(main) }.associate { it.symbol to it }

    val allPairs = info.keys
    val coinVolumes = allPairs
            .filter { !excludedPairs.contains(it) }
            .map {
                CoinVolumes(
                        coin = it.removeSuffix(main),
                        dailyVolumes = daylyVolumes(api, it, time).take(days).toList()
                )
            }
            .filter { it.dailyVolumes.size == days }

    val coins = coinVolumes
            .filter {
                val maxVolumes = it.dailyVolumes.windowed(maxByDays) { it.max()!! }
                maxVolumes.all { it > minDayVolume }
            }
            .map { it.coin }

    val printList = listOf("USDT") + coins
    println(printList.joinToString(", ") { "\"$it\"" })
    println(printList.size)
}

private suspend fun daylyVolumes(client: BinanceAPI, coin: String, beforeTime: Long): ReceiveChannel<Double> {
    return hourlyVolumes(client, coin, beforeTime).chunked(24).map { it.sum() }
}

private suspend fun hourlyVolumes(client: BinanceAPI, coin: String, beforeTime: Long): ReceiveChannel<Double> = produce {
    var t = beforeTime
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