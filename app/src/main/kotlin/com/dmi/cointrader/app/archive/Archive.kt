package com.dmi.cointrader.app.archive

import com.dmi.cointrader.app.binance.Asset
import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.trade.TradeConfig
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
import java.time.Instant

typealias History = List<Spreads>
typealias HistoryBatch = List<History>

interface Archive {
    suspend fun historyAt(range: PeriodRange): History
    suspend fun sync(currentTime: Instant)
}

@Serializable
private data class BinanceTradeConfig(val market: String)

@Serializable
private data class HistoryConfig(val periods: Periods, val assets: List<Asset>)

suspend fun archive(
        config: TradeConfig,
        exchange: BinanceExchange,
        currentTime: Instant,
        fileSystem: FileSystem = FileSystems.getDefault(),
        tradeLoadChunk: Int = 500,
        momentsReloadCount: Int = 10
): Archive {
    fun tradeAppendedLog(asset: String) = object : SyncFileList.Log<Trade> {
        override fun itemsAppended(items: List<Trade>, indices: LongRange) {
            val lastTradeTime = items.last().time
            println("Trade cached: $asset $lastTradeTime")
        }
    }

    fun spreadsAppendedLog() = object : SyncFileList.Log<Spreads> {
        override fun itemsAppended(items: List<Spreads>, indices: LongRange) {
            val endPeriod = Period(indices.last.toInt()).next()
            val time = config.periods.timeOf(endPeriod)
            println("Moment cached: $time")
        }
    }

    fun Instant.period() = config.periods.of(this)

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
            val market = market
            val list = if (isReversed) {
                list
            } else {
                list.map(Trade::reverse)
            }
            suspend fun sync(source: RestorableSource<BinanceTradeState, Trade>, log: SyncFileList.Log<Trade>) = list.sync(source, log)
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
            HistoryConfig.serializer(),
            PeriodicalState.serializer(
                    ScanState.serializer(
                            LongSerializer,
                            TimeSpread.serializer()
                    )
            ).list,
            FixedListSerializer(config.assets.alts.size, SpreadFixedSerializer),
            HistoryConfig(config.periods, config.assets.alts),
            reloadCount = momentsReloadCount
    )

    trades.forEach {
        it.sync(
                BinanceTrades(it.market, currentTime, tradeLoadChunk),
                tradeAppendedLog(it.asset)
        )
    }
    spreadsList.sync(
            spreadsSource(currentTime.period()),
            spreadsAppendedLog()
    )

    return object : Archive {
        var lastPeriod = currentTime.period()

        override suspend fun historyAt(range: PeriodRange): History {
            require(range.endInclusive <= lastPeriod)
            return spreadsList.get(range.nums().toLong())
        }

        override suspend fun sync(currentTime: Instant) {
            val period = currentTime.period()
            trades.forEach {
                it.sync(
                        BinanceTrades(it.market, currentTime, tradeLoadChunk),
                        EmptyLog()
                )
            }
            spreadsList.sync(
                    spreadsSource(period),
                    EmptyLog()
            )
            lastPeriod = period
        }
    }
}