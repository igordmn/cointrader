package com.dmi.cointrader.app.trade

import com.binance.api.client.domain.market.AggTrade
import com.dmi.util.atom.ReadAtom
import com.dmi.util.collection.Row
import com.dmi.util.collection.Table
import com.dmi.util.concurrent.buildChannel
import com.dmi.util.concurrent.map
import com.dmi.util.io.SyncTable
import com.dmi.util.io.syncFileTable
import exchange.binance.MarketInfo
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

typealias BinanceTradeId = Long
typealias BinanceTradeRow = Row<BinanceTradeId, Trade>

@Serializable
data class BinanceTradeConfig(val market: String)

class BinanceTrades(
        private val api: BinanceAPI,
        private var currentTime: ReadAtom<Instant>,
        private val market: String
) : Table<BinanceTradeId, Trade> {
    override fun rowsAfter(id: BinanceTradeId?): ReceiveChannel<Row<BinanceTradeId, Trade>> = buildChannel {
        val currentTime = currentTime()
        val startId: Long = 1 + (id ?: -1)
        binanceTrades(startId).takeWhile { it.value.time <= currentTime }
    }

    private fun binanceTrades(startId: Long): ReceiveChannel<BinanceTradeRow> = produce {
        val count = 500
        var id = startId

        while (true) {
            val trades = api.getAggTrades(market, id.toString(), count, null, null)
            if (trades.isNotEmpty()) {
                trades.forEach {
                    send(it.toRow())
                }
                id = trades.last().aggregatedTradeId + 1
            } else {
                break
            }
        }
    }

    private fun AggTrade.toRow() = BinanceTradeRow(
            aggregatedTradeId,
            Trade(
                    Instant.ofEpochMilli(tradeTime),
                    quantity.toDouble(),
                    price.toDouble()
            )
    )
}

suspend fun trades(
        api: BinanceAPI,
        currentTime: ReadAtom<Instant>,
        marketInfo: MarketInfo
): SyncTable<Trade> {
    val market = marketInfo.name
    val path = Paths.get("data/cache/binance/trades/$market")
    val source = BinanceTrades(api, currentTime, market)
    val original = syncFileTable(
            path,
            BinanceTradeConfig.serializer(),
            LongSerializer,
            TradeFixedSerializer,
            BinanceTradeConfig(market),
            source
    )

    return if (marketInfo.isReversed) {
        original.reversed()
    } else {
        original
    }
}

private fun SyncTable<Trade>.reversed() = object :SyncTable<Trade> {
    suspend override fun sync() = this@reversed.sync()

    override fun rowsAfter(id: Long?) = this@reversed.rowsAfter(id).map {
        Row(it.id, it.value.reverse())
    }
}