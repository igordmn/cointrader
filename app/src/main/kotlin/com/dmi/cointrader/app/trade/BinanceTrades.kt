package com.dmi.cointrader.app.trade

import com.binance.api.client.domain.market.AggTrade
import com.dmi.util.io.IdentitySource
import com.dmi.util.io.Indexed
import com.dmi.util.io.NumIdIndex
import com.dmi.util.io.SyncFileArray
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

typealias BinanceTradeId = Long
typealias TradeId = Long

typealias BinanceTradeIndex = NumIdIndex<BinanceTradeId>
typealias TradeIndex = NumIdIndex<TradeId>
typealias TradeItem = Indexed<TradeIndex, Trade>

@Serializable
data class BinanceTradeConfig(val market: String)

fun binanceTradeArray(path: Path) = SyncFileArray(
        path,
        BinanceTradeConfig.serializer(),
        TODO() as KSerializer<NumIdIndex<Long>>,
        TradeFixedSerializer()
)

class BinanceTradeSource(
        override val config: BinanceTradeConfig,
        var currentTime: Instant
) : IdentitySource<BinanceTradeConfig, BinanceTradeIndex, Trade> {
    override fun after(lastIndex: BinanceTradeIndex?): ReceiveChannel<TradeItem> {
        val startIndex = lastIndex ?: BinanceTradeIndex(0, 0)
        return getTrades(startIndex, currentTime)
    }
}

fun binanceTrades(
        api: BinanceAPI,
        market: String,
        startId: Long,
        beforeTime: Instant
): ReceiveChannel<BinanceTrade> = binanceTrades(api, market, startId).takeWhile { it.trade.time <= beforeTime }

private fun binanceTrades(
        api: BinanceAPI,
        market: String,
        startId: Long
): ReceiveChannel<BinanceTrade> = produce {
    val count = 500
    var id = startId

    while(true) {
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

private fun AggTrade.toBinanceTrade() = BinanceTrade(
        aggregatedTradeId,
        Trade(
                Instant.ofEpochMilli(tradeTime),
                quantity.toDouble(),
                price.toDouble()
        )
)