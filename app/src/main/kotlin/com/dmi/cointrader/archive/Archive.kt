package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.binance.BinanceExchange
import com.dmi.cointrader.trade.TradeAssets
import com.dmi.util.collection.SuspendList
import com.dmi.util.collection.toInt
import com.dmi.util.concurrent.forEachAsync
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
import java.sql.DriverManager
import java.time.Instant

typealias Spreads = List<Spread>

interface Archive : SuspendList<Spreads> {
    suspend fun sync(currentPeriod: Period)
}

private typealias Prices = Array<Double>

suspend fun archive2(
        space: PeriodSpace,
        assets: TradeAssets,
        exchange: BinanceExchange,
        currentPeriod: Period,
        fileSystem: FileSystem = FileSystems.getDefault(),
        reloadCount: Int,
        tradeLoadChunk: Int = 500
): Archive {
    val prices = Array((currentPeriod + 1).toInt()) { DoubleArray(assets.alts.size) { -1.0 } }

    val nameToIndex: Map<Asset, Int> = assets.alts.indices.associate { assets.alts[it] to it }

    Class.forName("org.sqlite.JDBC")
    val connection = DriverManager.getConnection("jdbc:sqlite:D:/Development/Projects/cointrader/coins.db")
    connection.prepareStatement("SELECT coin, closeTime, close FROM History where exchange=\"binance\"").use {
        it.fetchSize = 1000
        it.executeQuery().use { rs ->
            while (rs.next()) {
                val coin = rs.getString(1)
                val closeTime = Instant.ofEpochSecond(rs.getLong(2))
                val close = rs.getDouble(3)
                val index = nameToIndex[coin]
                if (index != null) {
                    val period = space.floor(closeTime)
                    if (period <= currentPeriod) {
                        prices[period.toInt()][index] = close
                    }
                }
            }
        }
    }

    for (a in assets.alts.indices) {
        for (i in 1..currentPeriod.toInt()) {
            if (prices[i][a] < 0) {
                prices[i][a] = prices[i - 1][a]
            }
        }
    }

    for (a in assets.alts.indices) {
        for (i in currentPeriod.toInt() - 1 downTo 0) {
            if (prices[i][a] < 0) {
                prices[i][a] = prices[i + 1][a]
            }
        }
    }

    return object : Archive {
        override suspend fun size(): Long = prices.size.toLong()
        override suspend fun get(range: LongRange): List<Spreads> = prices.slice(range.toInt()).map { it.map { Spread(it, it) } }

        override suspend fun sync(currentPeriod: Period) = Unit
    }
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
    fun logTradeAppended(asset: String) = object : SyncFileList.Log<Trade> {
        override fun itemsAppended(items: List<Trade>, indices: LongRange) {
            val lastTradeTime = items.last().time
            println("Trades cached: $asset $lastTradeTime")
        }
    }

    fun logSpreadsAppended() = object : SyncFileList.Log<Spreads> {
        override fun itemsAppended(items: List<Spreads>, indices: LongRange) {
            val endPeriod = indices.last + 1
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

    fun SuspendList<Trade>.spreadSource(currentPeriod: Period) = asRestorableSource(bufferSize = 30000)
            .spreads()
            .periodical(space)
            .takeWhile { it.period <= currentPeriod }
            .map { it.spread }

    fun spreadsSource(currentPeriod: Period) = trades
            .map { it.list.spreadSource(currentPeriod) }
            .zip()

    val spreadsList = syncFileList(
            spreadsFile,
            SpreadsSourceConfig.serializer(),
            PeriodicalState.serializer(spreadsStateSerializer(LongSerializer)).list,
            FixedListSerializer(assets.alts.size, SpreadFixedSerializer),
            SpreadsSourceConfig(space, assets.alts),
            bufferSize = 10000,
            reloadCount = reloadCount
    )

    trades.forEachAsync {
        it.sync(
                TradeSource(it.market, currentPeriod.time(), tradeLoadChunk),
                logTradeAppended(it.asset)
        )
    }
    spreadsList.sync(
            spreadsSource(currentPeriod),
            logSpreadsAppended()
    )

    return object : Archive {
        override suspend fun size(): Long = spreadsList.size()
        override suspend fun get(range: LongRange): List<Spreads> = spreadsList.get(range)

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
        }
    }
}