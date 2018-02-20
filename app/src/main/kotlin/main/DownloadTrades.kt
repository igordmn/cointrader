package main

import data.TradeCache
import dataOld.DOWNLOAD_COINS
import dataOld.downloadPair
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import util.concurrent.mapAsync
import java.nio.file.Paths
import java.time.Instant

val dbPath = Paths.get("data/cache/binance.db")
val endTime = Instant.now()

fun main(args: Array<String>) {

    runBlocking {
        TradeCache.create(dbPath).use { cache ->
            DOWNLOAD_COINS.windowed(5).forEach { window ->
                window.mapAsync {
                    val pair = downloadPair(it)
                    download(pair, cache)
                }
            }
        }
    }
}

private suspend fun download(market: String, cache: TradeCache) {
    val api = binanceAPI()

    var id = (cache.lastTradeId(market) ?: -1) + 1L
    while (true) {
        val trades = api.getAggTrades(market, id.toString(), 500, null, null)
        if (trades.isEmpty() || Instant.ofEpochMilli(trades.last().tradeTime) > endTime) {
            break
        }
        println(market + " " + Instant.ofEpochMilli(trades.first().tradeTime))

        val filteredTrades = trades.filter {
            it.isBestPrice
        }
        cache.insertTrades(market, filteredTrades)
        id = trades.last().aggregatedTradeId + 1
    }
}