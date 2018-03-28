package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.binance.BinanceExchange
import com.dmi.cointrader.trade.TradeAssets
import com.dmi.util.collection.SuspendList
import com.dmi.util.collection.chunked
import com.dmi.util.collection.toLong
import com.dmi.util.concurrent.flatten
import com.dmi.util.concurrent.forEachAsync
import com.dmi.util.io.FixedListSerializer
import com.dmi.util.io.SyncFileList
import com.dmi.util.io.SyncFileList.EmptyLog
import com.dmi.util.io.syncFileList
import com.dmi.util.restorable.*
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import kotlinx.serialization.list
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files.createDirectories

typealias Spreads = List<Spread>

interface Archive {
    fun historyAt(range: PeriodRange): ReceiveChannel<Spreads>
    suspend fun sync(currentPeriod: Period)
}

@Serializable
private data class TradeSourceConfig(val market: String)

@Serializable
private data class SpreadsSourceConfig(val periodSpace: PeriodSpace, val assets: List<Asset>)

suspend fun archive(
        space: PeriodSpace,
        assets: TradeAssets,
        exchange: BinanceExchange,
        currentPeriod: Period,
        fileSystem: FileSystem = FileSystems.getDefault(),
        reloadCount: Int,
        tradeLoadChunk: Int = 500
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
            val time = space.timeOf(endPeriod)
            println("Spreads cached: $time")
        }
    }

    fun Period.time() = space.timeOf(this)

    val cacheDir = fileSystem.getPath("data/cache/binance")
    val tradesDir = cacheDir.resolve("trades")
    val spreadsFile = cacheDir.resolve("spreads")
    createDirectories(cacheDir)
    createDirectories(tradesDir)

    val mainAsset = assets.main
    val trades = assets.alts.map { asset ->
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
            .periodical(space)
            .takeWhile { it.period <= currentPeriod }
            .map { it.spread }

    fun spreadsSource(currentPeriod: Period) = trades
            .map { it.list.spreadSource(currentPeriod) }
            .zip()

    val spreadsList = syncFileList(
            spreadsFile,
            SpreadsSourceConfig.serializer(),
            PeriodicalState.serializer(
                    ScanState.serializer(
                            LongSerializer,
                            TimeSpread.serializer()
                    )
            ).list,
            FixedListSerializer(assets.alts.size, SpreadFixedSerializer),
            SpreadsSourceConfig(space, assets.alts),
            bufferSize = 10000,
            reloadCount = reloadCount
    )

    trades.forEachAsync {
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

        override fun historyAt(range: PeriodRange): ReceiveChannel<Spreads> {
            require(range.endInclusive <= this.currentPeriod)
            return range
                        .chunked(size = 10000)
                        .asReceiveChannel()
                        .map { spreadsList.get(it.toLong()) }
                        .flatten()
        }

        override suspend fun sync(currentPeriod: Period) {
            trades.forEachAsync {
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