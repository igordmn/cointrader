package com.dmi.cointrader.app.trade

import com.binance.api.client.domain.market.AggTrade
import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.moment.MomentId
import com.dmi.util.collection.Indexed
import com.dmi.util.collection.NumIdIndex
import com.dmi.util.collection.SuspendArray
import com.dmi.util.io.SyncSource
import com.dmi.util.io.SyncFileArray
import exchange.binance.BinanceConstants
import exchange.binance.MarketInfo
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import main.test.Config
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

typealias BinanceTradeId = Long
typealias TradeId = Long

typealias BinanceTradeIndex = NumIdIndex<BinanceTradeId>
typealias TradeIndex = NumIdIndex<TradeId>
typealias TradeItem = Indexed<TradeIndex, Trade>

val tradeIndexSerializer = NumIdIndex.serializer(LongSerializer)

@Serializable
data class BinanceTradeConfig(val market: String)

fun binanceTradeArray(path: Path) = SyncFileArray(
        path,
        BinanceTradeConfig.serializer(),
        tradeIndexSerializer,
        TradeFixedSerializer()
)

class BinanceTradesSource(
        private val api: BinanceAPI,
        private val market: String,
        override val config: BinanceTradeConfig,
        var currentTime: Instant
) : SyncSource<BinanceTradeConfig, BinanceTradeIndex, Trade> {
    override fun newItems(lastIndex: BinanceTradeIndex?): ReceiveChannel<TradeItem> {
        val startNum: Long
        val startId: Long
        if (lastIndex != null) {
            startNum = lastIndex.num + 1
            startId = lastIndex.id + 1
        } else {
            startNum = 0L
            startId = 0L
        }

        return binanceTrades(api, market, startId, currentTime).mapIndexed { num, trade ->
            TradeItem(TradeIndex(startNum + num, trade.id), trade.trade)
        }
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

private fun AggTrade.toBinanceTrade() = BinanceTrade(
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
    val originalArray = binanceTradeArray(Paths.get("data/cache/binance/trades/$market"))

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