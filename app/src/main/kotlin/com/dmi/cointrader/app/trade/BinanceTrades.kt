package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.binance.BinanceConstants
import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.binance.MarketInfo
import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.util.atom.ReadAtom
import com.dmi.util.concurrent.buildChannel
import com.dmi.util.concurrent.map
import com.dmi.util.io.RestorableSource
import com.dmi.util.io.SyncList
import com.dmi.util.io.syncFileList
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.serialization.Serializable
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
        private val market: BinanceExchange.Market,
        private var currentTime: ReadAtom<Instant>
) : RestorableSource<BinanceTradeState, Trade> {
    override fun restore(state: BinanceTradeState?): ReceiveChannel<BinanceTradeItem> = buildChannel {
        val currentTime = currentTime()
        val startId = if (state != null) state.id + 1L else 0L
        market.trades(startId).map(::convert).takeWhile { it.value.time <= currentTime }
    }

    private fun convert(trade: BinanceExchange.Trade) = BinanceTradeItem(
            BinanceTradeState(trade.aggTradeId),
            Trade(
                    trade.time,
                    trade.amount.toDouble(),
                    trade.price.toDouble()
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
    val path = Paths.get("old/data/cache/binance/trades")
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
    val source = BinanceTrades(TODO(), currentTime)
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