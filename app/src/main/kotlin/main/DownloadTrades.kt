package main

import com.binance.api.client.domain.market.AggTrade
import dataOld.DOWNLOAD_COINS
import dataOld.downloadPair
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.runBlocking
import util.concurrent.mapAsync
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant

val root = Paths.get("D:\\Development\\Projects\\cointrader\\trades")
val endTime = Instant.now()

fun main(args: Array<String>) {
    runBlocking {
        DOWNLOAD_COINS.windowed(5).forEach { window ->
            window.mapAsync {
                val pair = downloadPair(it)
                download(pair)
            }
        }
    }
}

private suspend fun download(market: String) {
    val api = binanceAPI()

    var id = readLastId(market) + 1
    while(true) {
        val trades = api.getAggTrades(market, id.toString(), 500, null, null)
        if (trades.isEmpty() || Instant.ofEpochMilli(trades.last().tradeTime) > endTime) {
            break
        }
        println(market + " " + Instant.ofEpochMilli(trades.first().tradeTime))
        write(market, trades)
        val lastId = trades.last().aggregatedTradeId
        writeLastId(market, lastId)
        id = lastId + 1
    }
}

private fun write(market: String, trades: List<AggTrade>) {
    val path = root.resolve(Paths.get(market))
    val data = toBytes(trades)
    if (!Files.exists(path)) {
        Files.createFile(path)
    }
    Files.write(path, data, StandardOpenOption.APPEND)
}

private fun writeLastId(market: String, id: Long) {
    val path = root.resolve(Paths.get(market + "_lastId"))
    ObjectOutputStream(path.toFile().outputStream()).use {
        it.writeLong(id)
    }
}

private fun readLastId(market: String): Long {
    val path = root.resolve(Paths.get(market + "_lastId"))
    return if (Files.exists(path)) {
        ObjectInputStream(path.toFile().inputStream()).use {
            it.readLong()
        }
    } else {
        -1
    }
}

private fun toBytes(trades: List<AggTrade>): ByteArray {
    val bs = ByteArrayOutputStream()
    ObjectOutputStream(bs).use { os ->
        trades.forEach {
            os.writeLong(it.tradeTime)
            os.writeDouble(it.quantity.toDouble())
            os.writeDouble(it.price.toDouble())
        }
    }
    return bs.toByteArray()
}

