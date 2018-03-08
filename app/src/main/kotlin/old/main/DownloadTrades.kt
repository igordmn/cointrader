package old.main

import old.data.TradeCache
import old.dataOld.DOWNLOAD_COINS
import old.dataOld.downloadPair
import old.exchange.binance.api.BinanceAPI
import old.exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import com.dmi.util.concurrent.mapAsync
import java.nio.file.Paths
import java.time.Instant

val dbPath = Paths.get("old/data/cache/binance.db")
val endTime = Instant.now()

fun main(args: Array<String>) {

    runBlocking {
        val api = binanceAPI()
        TradeCache.create(dbPath).use { cache ->
            DOWNLOAD_COINS.mapAsync {
                val pair = downloadPair(it)
                download(api, pair, cache)
            }
        }
    }
}

private suspend fun download(api: BinanceAPI, market: String, cache: TradeCache) {
    var id = (cache.lastTradeId(market) ?: -1) + 1L
    while (true) {
        val trades = api.getAggTrades(market, id.toString(), 500, null, null)
        if (trades.isEmpty() || Instant.ofEpochMilli(trades.last().tradeTime) > endTime || trades.size != 500) {
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