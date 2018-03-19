package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.Asset
import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.PeriodRange
import com.dmi.cointrader.app.trade.TradeConfig
import com.dmi.util.collection.rangeMap
import com.dmi.util.io.SyncFileList
import com.dmi.util.io.syncFileList
import java.nio.file.Files.createDirectories
import java.time.Instant
import com.dmi.util.io.SyncFileList.EmptyLog
import java.nio.file.FileSystem

typealias History = List<Moment>
typealias HistoryBatch = List<History>

interface Archive {
    suspend fun historyAt(range: PeriodRange): History
    suspend fun sync(currentTime: Instant)
}

suspend fun archive(
        fileSystem: FileSystem,
        config: TradeConfig,
        exchange: BinanceExchange,
        currentTime: Instant
): Archive {
    fun tradeAppendLog(asset: String) = object : SyncFileList.Log<Trade> {
        override fun itemsAppended(items: List<Trade>, indices: LongRange) {
            val lastTradeTime = items.last().time
            println("Trade cached: $asset $lastTradeTime")
        }
    }

    fun momentAppendLog() = object : SyncFileList.Log<Moment> {
        override fun itemsAppended(items: List<Moment>, indices: LongRange) {
            val endPeriod = Period(indices.last).next()
            val time = config.periods.startOf(endPeriod)
            println("Moment cached: $time")
        }
    }

    val cacheDir = fileSystem.getPath("data/cache/binance")
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
        object {
            val asset = asset
            val isReversed = isReversed
            val market = market
            val list = list
        }
    }

    val tradeLists = trades.map {
        if (it.isReversed) {
            it.list
        } else {
            it.list.map(Trade::reverse)
        }
    }
    val momentsList = syncFileList(
            momentsFile,
            MomentsConfig.serializer(),
            MomentState.serializer(),
            MomentFixedSerializer(config.assets.alts.size),
            MomentsConfig(config.periods, config.assets.alts)
    )

    trades.forEach {
        it.list.sync(
                BinanceTrades(it.market, currentTime),
                tradeAppendLog(it.asset)
        )
    }
    momentsList.sync(
            TradeMoments(config.periods, tradeLists, currentTime),
            momentAppendLog()
    )

    return object : Archive {
        suspend override fun historyAt(range: PeriodRange): History = momentsList.get(range.start.num..range.endInclusive.num)

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