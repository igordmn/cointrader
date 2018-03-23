package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.binance.BinanceExchange
import com.dmi.cointrader.trade.TradeConfig
import com.dmi.util.collection.SuspendList
import com.dmi.util.collection.toLong
import com.dmi.util.io.FixedListSerializer
import com.dmi.util.io.SyncFileList
import com.dmi.util.io.SyncFileList.EmptyLog
import com.dmi.util.io.syncFileList
import com.dmi.util.restorable.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import kotlinx.serialization.list
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files.createDirectories

typealias Spreads = List<Spread>
typealias SpreadsBatch = List<Spreads>
typealias History = List<Spreads>
typealias HistoryBatch = List<History>

interface Archive {
    suspend fun historyAt(range: PeriodRange): History
    suspend fun sync(currentPeriod: Period)
}

@Serializable
private data class TradeSourceConfig(val market: String)

@Serializable
private data class SpreadsSourceConfig(val periods: Periods, val assets: List<Asset>)

suspend fun archive(
        config: TradeConfig,
        exchange: BinanceExchange,
        currentPeriod: Period,
        fileSystem: FileSystem = FileSystems.getDefault(),
        tradeLoadChunk: Int = 500,
        reloadCount: Int = 10
): Archive {
    fun tradeAppendedLog(asset: String) = object : SyncFileList.Log<Trade> {
        override fun itemsAppended(items: List<Trade>, indices: LongRange) {
            val lastTradeTime = items.last().time
            println("Trade cached: $asset $lastTradeTime")
        }
    }

    fun spreadsAppendedLog() = object : SyncFileList.Log<Spreads> {
        override fun itemsAppended(items: List<Spreads>, indices: LongRange) {
            val endPeriod = indices.last.toInt() + 1
            val time = config.periods.timeOf(endPeriod)
            println("Moment cached: $time")
        }
    }

    fun Period.time() = config.periods.timeOf(this)

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
                TradeSourceConfig.serializer(),
                TradeState.serializer(),
                TradeFixedSerializer,
                TradeSourceConfig(name)
        )
        object {
            val asset = asset
            val market = market
            val list = if (isReversed) {
                list
            } else {
                list.map(Trade::reverse)
            }
            suspend fun sync(source: RestorableSource<TradeState, Trade>, log: SyncFileList.Log<Trade>) = list.sync(source, log)
        }
    }

    fun SuspendList<Trade>.spreadSource(currentPeriod: Period) = asRestorableSource()
            .scan(Trade::initialSpread, Trade::nextSpread)
            .periodical(config.periods)
            .takeWhile { it.period <= currentPeriod }
            .map { it.spread }

    fun spreadsSource(currentPeriod: Period) = trades
            .map { it.list.spreadSource(currentPeriod) }
            .zip()

    val spreadsList = syncFileList(
            momentsFile,
            SpreadsSourceConfig.serializer(),
            PeriodicalState.serializer(
                    ScanState.serializer(
                            LongSerializer,
                            TimeSpread.serializer()
                    )
            ).list,
            FixedListSerializer(config.assets.alts.size, SpreadFixedSerializer),
            SpreadsSourceConfig(config.periods, config.assets.alts),
            bufferSize = 4096,
            reloadCount = reloadCount
    )

    trades.forEach {
        it.sync(
                TradeSource(it.market, currentPeriod.time(), tradeLoadChunk),
                tradeAppendedLog(it.asset)
        )
    }
    spreadsList.sync(
            spreadsSource(currentPeriod),
            spreadsAppendedLog()
    )

    return object : Archive {
        var currentPeriod = currentPeriod

        override suspend fun historyAt(range: PeriodRange): History {
            require(range.endInclusive <= this.currentPeriod)
            return spreadsList.get(range.toLong())
        }

        override suspend fun sync(currentPeriod: Period) {
            trades.forEach {
                it.sync(
                        TradeSource(it.market, currentPeriod.time(), tradeLoadChunk),
                        EmptyLog()
                )
            }
            spreadsList.sync(
                    spreadsSource(currentPeriod),
                    EmptyLog()
            )
            this.currentPeriod = currentPeriod
        }
    }
}