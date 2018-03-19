package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.trade.TradeConfig
import com.dmi.util.io.SyncFileList
import com.dmi.util.io.syncFileList
import java.nio.file.Files.createDirectories
import java.nio.file.Paths
import java.time.Instant
import com.dmi.util.io.SyncFileList.EmptyLog

typealias History = List<Moment>

interface Archive {
    suspend fun historyAt(period: Period, size: Int): History
    suspend fun sync(currentTime: Instant)
}

suspend fun archive(config: TradeConfig, exchange: BinanceExchange, currentTime: Instant): Archive {
    val cacheDir = Paths.get("data/cache/binance")
    val tradesDir = cacheDir.resolve("trades")
    val momentsFile = cacheDir.resolve("moments")
    createDirectories(cacheDir)
    createDirectories(tradesDir)

    val mainAsset = config.assets.main
    val trades = config.assets.alts.map { asset ->
        val normalMarket = exchange.market(mainAsset, asset)
        val reversedMarket = exchange.market(asset, mainAsset)
        val market = normalMarket ?: reversedMarket!!
        val isReversed = normalMarket == null
        val name = if (isReversed) "$asset$mainAsset" else "$mainAsset$asset"

        val list = syncFileList(
                tradesDir.resolve(name),
                BinanceTradeConfig.serializer(),
                BinanceTradeState.serializer(),
                TradeFixedSerializer,
                BinanceTradeConfig(name)
        )
        list.sync(
                BinanceTrades(market, currentTime),
                tradeAppendLog(asset)
        )
        TradesArchive(market, list, isReversed)
    }

    val tradeLists = trades.map {
        if (it.isReversed) {
            it.list.map(Trade::reverse)
        } else {
            it.list
        }
    }
    val momentsList = syncFileList(
            momentsFile,
            MomentsConfig.serializer(),
            MomentState.serializer(),
            MomentFixedSerializer(config.assets.alts.size),
            MomentsConfig(config.periods, config.assets.alts)
    )
    momentsList.sync(
            TradeMoments(config.periods, tradeLists, currentTime),
            EmptyLog()
    )

    return object : Archive {
        suspend override fun historyAt(period: Period, size: Int): History {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        suspend override fun sync(currentTime: Instant) {
            trades.forEach {
                it.list.sync(
                        BinanceTrades(it.market, currentTime),
                        EmptyLog()
                )
            }
            momentsList.sync(
                    TradeMoments(config.periods, tradeLists, currentTime),
                    EmptyLog()
            )
        }
    }
}

private class TradesArchive(val market: BinanceExchange.Market, val list: SyncFileList<BinanceTradeState, Trade>, val isReversed: Boolean)

private fun tradeAppendLog(asset: String) = object: SyncFileList.Log<Trade> {
    override fun itemsAppended(items: List<Trade>, indices: LongRange) {
        val lastTradeTime = items.last().time
        println("$asset $lastTradeTime")
    }
}