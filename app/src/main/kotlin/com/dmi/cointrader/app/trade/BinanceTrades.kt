package com.dmi.cointrader.app.trade

import com.binance.api.client.domain.market.AggTrade
import com.dmi.util.collection.Row
import com.dmi.util.collection.IdIndex
import com.dmi.util.collection.SuspendArray
import com.dmi.util.collection.Table
import com.dmi.util.io.SyncSource
import com.dmi.util.io.SyncFileTable
import com.dmi.util.io.syncFileTable
import exchange.binance.MarketInfo
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

typealias BinanceTradeId = Long
typealias BinanceTradeRow = Row<BinanceTradeId, Trade>

@Serializable
data class BinanceTradeConfig(val market: String)

suspend fun tradesFileTable(path: Path) = syncFileTable(
        path,
        BinanceTradeConfig.serializer(),
        LongSerializer,
        TradeFixedSerializer
)

class BinanceTrades(
        private val api: BinanceAPI,
        private val market: String,
        var currentTime: Instant
) : Table<BinanceTradeId, Trade> {
    override fun rowsAfter(id: BinanceTradeId?): ReceiveChannel<Row<BinanceTradeId, Trade>> {
        val startId: Long = 1 + (id ?: -1)
        return binanceTrades(api, market, startId, currentTime)
    }
}

fun binanceTrades(
        api: BinanceAPI,
        market: String,
        startId: Long,
        beforeTime: Instant
): ReceiveChannel<BinanceTradeRow> = binanceTrades(api, market, startId).takeWhile { it.value.time <= beforeTime }

private fun binanceTrades(
        api: BinanceAPI,
        market: String,
        startId: Long
): ReceiveChannel<BinanceTradeRow> = produce {
    val count = 500
    var id = startId

    while (true) {
        val trades = api.getAggTrades(market, id.toString(), count, null, null)
        if (trades.isNotEmpty()) {
            trades.forEach {
                send(it.toBinanceTrade())
            }
            id = trades.last().aggregatedTradeId + 1
        } else {
            break
        }
    }
}

private fun AggTrade.toBinanceTrade() = BinanceTradeRow(
        aggregatedTradeId,
        Trade(
                Instant.ofEpochMilli(tradeTime),
                quantity.toDouble(),
                price.toDouble()
        )
)

interface Trades : SuspendArray<Trade> {
    suspend fun sync(currentTime: Instant)
}

suspend fun binanceTrades(
        api: BinanceAPI,
        market: MarketInfo,
        currentTime: Instant
): Trades {
    val tradeConfig = BinanceTradeConfig(market.name)
    val source = BinanceTradesSource(api, market.name, tradeConfig, currentTime)
    val originalArray = tradesFileTable(Paths.get("data/cache/binance/trades/$market"))

    originalArray.syncWith(source)

    val array = if (market.isReversed) {
        originalArray.map(Trade::reverse)
    } else {
        originalArray
    }

    return object : Trades {
        override suspend fun sync(currentTime: Instant) {
            source.currentTime = currentTime
            originalArray.syncWith(source)
        }

        override val size: Long = array.size
        suspend override fun get(range: LongRange): List<Trade> = array.get(range)
    }
}