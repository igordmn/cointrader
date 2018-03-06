package com.dmi.cointrader.app.trade

import com.binance.api.client.domain.market.AggTrade
import com.dmi.util.atom.ReadAtom
import com.dmi.util.concurrent.buildChannel
import com.dmi.util.io.RestorableSource
import com.dmi.util.io.SyncList
import com.dmi.util.io.appendToFileName
import com.dmi.util.io.syncFileList
import exchange.binance.BinanceConstants
import exchange.binance.MarketInfo
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.serialization.Serializable
import main.test.Config
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

@Serializable
class BinanceTradeState(val id: Long)

typealias BinanceTradeItem = RestorableSource.Item<BinanceTradeState, Trade>

@Serializable
data class BinanceTradeConfig(val market: String)

class BinanceTrades(
        private val api: BinanceAPI,
        private var currentTime: ReadAtom<Instant>,
        private val market: String,
        private val chunkLoadCount: Int = 500
) : RestorableSource<BinanceTradeState, Trade> {
    override fun restore(state: BinanceTradeState?): ReceiveChannel<BinanceTradeItem> = buildChannel {
        val currentTime = currentTime()
        val startId = if (state != null) state.id + 1L else 0L
        binanceTrades(startId).takeWhile { it.value.time <= currentTime }
    }

    private fun binanceTrades(startId: Long): ReceiveChannel<BinanceTradeItem> = produce {
        var id = startId

        while (true) {
            val trades = api.getAggTrades(market, id.toString(), chunkLoadCount, null, null)
            if (trades.isNotEmpty()) {
                trades.forEach {
                    send(it.toItem())
                }
                id = trades.last().aggregatedTradeId + 1
            } else {
                break
            }
        }
    }

    private fun AggTrade.toItem() = BinanceTradeItem(
            BinanceTradeState(aggregatedTradeId),
            Trade(
                    Instant.ofEpochMilli(tradeTime),
                    quantity.toDouble(),
                    price.toDouble()
            )
    )
}

suspend fun coinToCachedBinanceTrades(
        config: Config,
        constants: BinanceConstants,
        api: BinanceAPI,
        currentTime: ReadAtom<Instant>,
        coinLog: (coin: String) -> SyncList.Log<Trade> = { SyncList.EmptyLog() }
): List<SyncList<Trade>> {
    val path = Paths.get("data/cache/binance")
    Files.createDirectories(path)
    return coinToCachedBinanceTrades(config.mainCoin, config.altCoins, path, constants, api, currentTime, coinLog)
}

suspend fun coinToCachedBinanceTrades(
        mainCoin: String,
        altCoins: List<String>,
        path: Path,
        constants: BinanceConstants,
        api: BinanceAPI,
        currentTime: ReadAtom<Instant>,
        coinLog: (coin: String) -> SyncList.Log<Trade> = { SyncList.EmptyLog() },
        chunkLoadCount: Int = 500
): List<SyncList<Trade>> {
    return altCoins.map { coin ->
        val marketInfo = constants.marketInfo(coin, mainCoin)
        cachedBinanceTrades(api, currentTime, path.resolve(marketInfo.name), marketInfo, coinLog(coin), chunkLoadCount)
    }
}

suspend fun cachedBinanceTrades(
        api: BinanceAPI,
        currentTime: ReadAtom<Instant>,
        path: Path,
        marketInfo: MarketInfo,
        log: SyncList.Log<Trade> = SyncList.EmptyLog(),
        chunkLoadCount: Int
): SyncList<Trade> {
    val market = marketInfo.name
    val source = BinanceTrades(api, currentTime, market, chunkLoadCount)
    val original: SyncList<Trade> = syncFileList(
            path,
            BinanceTradeConfig.serializer(),
            BinanceTradeState.serializer(),
            TradeFixedSerializer,
            BinanceTradeConfig(market),
            source,
            log
    )

    return if (marketInfo.isReversed) {
        original.map(Trade::reverse)
    } else {
        original
    }
}