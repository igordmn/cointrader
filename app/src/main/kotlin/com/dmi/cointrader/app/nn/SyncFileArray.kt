package com.dmi.cointrader.app.nn

import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.binanceTrades
import com.dmi.util.concurrent.chunked
import com.dmi.util.concurrent.flatten
import com.dmi.util.io.*
import exchange.binance.BinanceConstants
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class MarketInfo(val coin: String, val name: String, val isReversed: Boolean)

fun marketInfo(coin: String): MarketInfo {
    val mainCoin = "BTC"
    val constants = BinanceConstants()

    val name = constants.marketName(coin, mainCoin)
    val reversedName = constants.marketName(mainCoin, coin)

    return when {
        name != null -> MarketInfo(coin, name, false)
        reversedName != null -> MarketInfo(coin, reversedName, true)
        else -> throw UnsupportedOperationException()
    }
}

fun main(args: Array<String>) {
    runBlocking {
        val api = binanceAPI()
        val constants = BinanceConstants()
        val startTime = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3"))
        val period = Duration.ofMinutes(5)
        val coins: List<String> = listOf(
                "USDT", "ETH", "TRX", "XRP", "LTC", "ETC", "ICX"
        )

        val momentConfig = MomentConfig(startTime, period, coins)
        val currentTime = Instant.ofEpochMilli(api.serverTime().serverTime)

        coins.forEach { coin ->
            val market = marketInfo(coin)
            val binanceTrades = SyncFileArray(
                    Paths.get("D:/yy/trades/$market"),
                    BinanceTradeConfig.serializer(),
                    LongSerializer,
                    TradeSerializer()
            )
            fun getTrades(startId: BinanceAggTradeId, beforeTime: Instant): ReceiveChannel<TradeItem> {
                return binanceTrades(api, market.name, startId, beforeTime).map {
                    SyncFileArray.Source.Item(0, it.aggregatedId, it.trade)
                }
            }
            val binanceTradesSource = BinanceTradeSource(BinanceTradeConfig(market.name), currentTime, ::getTrades)
            binanceTrades.syncWith(binanceTradesSource)
        }
    }
}

interface SuspendArray<out T> {
    val size: Long

    suspend fun get(range: LongRange): List<T>
}

@Serializable
data class Candle(val close: Double, val high: Double, val low: Double)

@Serializable
data class Moment(val coinIndexToCandle: List<Candle>)

@Serializable
data class MomentConfig(val startTime: Instant, val period: Duration, val coins: List<String>)

@Serializable
data class CandleId(val firstTradeId: Long)

@Serializable
data class MomentId(val candles: List<CandleId>)

typealias BinanceAggTradeId = Long
typealias TradeId = Long
typealias TradeItem = SyncFileArray.Source.Item<TradeId, Trade>
typealias CandleItem = SyncFileArray.Source.Item<CandleId, Candle>
typealias MomentItem = SyncFileArray.Source.Item<MomentId, Moment>


fun periodIndex(startTime: Instant, period: Duration, time: Instant): Long {
    return Duration.between(time, startTime).toMillis() / period.toMillis()
}


// todo add pads and gaps
class CandleBuilder(private val startTime: Instant, private val period: Duration) {
    private val trades = ArrayList<TradeItem>()
    private var periodIndex: Long = -1

    fun addAndBuild(trade: TradeItem): CandleItem? {
        val periodIndex = periodIndex(startTime, period, trade.value.time)
        return if (trades.isNotEmpty()) {
            require(periodIndex >= this.periodIndex)
            if (periodIndex == this.periodIndex) {
                trades.add(trade)
                null
            } else {
                val candle = build()
                trades.clear()
                trades.add(trade)
                this.periodIndex = periodIndex
                candle
            }
        } else {
            trades.add(trade)
            this.periodIndex = periodIndex
            null
        }
    }

    fun buildLast(): CandleItem? = if (trades.isNotEmpty()) build() else null

    private fun build() = CandleItem(
            periodIndex.apply { require(this >= 0) },
            CandleId(trades.first().id),
            Candle(
                    trades.last().value.price,
                    trades.maxBy { it.value.price }!!.value.price,
                    trades.minBy { it.value.price }!!.value.price
            )
    )
}

private fun ReceiveChannel<TradeItem>.candles(
        startTime: Instant,
        endTime: Instant,
        period: Duration
): ReceiveChannel<CandleItem> = candlesWithTrades(startTime, endTime, period).candlesWithoutTrades(startTime, endTime, period)

private fun ReceiveChannel<TradeItem>.candlesWithTrades(
        startTime: Instant,
        endTime: Instant,
        period: Duration
): ReceiveChannel<CandleItem> = produce<CandleItem> {
    val candleBuilder = CandleBuilder(startTime, period)
    takeWhile { it.value.time < endTime }.consumeEach {
        val candle = candleBuilder.addAndBuild(it)
        if (candle != null) {
            send(candle)
        }
    }
    val lastCandle = candleBuilder.buildLast()
    if (lastCandle != null) {
        send(lastCandle)
    }
}

private fun ReceiveChannel<CandleItem>.candlesWithoutTrades(
        startTime: Instant,
        endTime: Instant,
        period: Duration
): ReceiveChannel<CandleItem> = produce {
    var last: CandleItem? = null
    val endIndex = periodIndex(startTime, period, endTime)

    consumeEach {
        val startIndex = if (last != null) last!!.index + 1 else 0
        require(it.index >= startIndex)

        for (i in startIndex until it.index) {
            send(CandleItem(i, it.id, it.value))
        }

        send(it)

        last = it
    }

    last?.let {
        for (i in it.index until endIndex) {
            send(CandleItem(i, it.id, it.value))
        }
    }
}

private fun List<ReceiveChannel<CandleItem>>.moments(): ReceiveChannel<MomentItem> {
    return zip().map { moment(it) }
}

private fun moment(candles: List<CandleItem>): MomentItem {
    require(candles.isNotEmpty())
    val index = candles.first().index
    candles.forEach {
        require(it.index == index)
    }
    val candleIds = candles.map { it.id }
    val candleValues = candles.map { it.value }
    val id = MomentId(candleIds)
    val value = Moment(candleValues)
    return MomentItem(index, id, value)
}

private fun <T> List<ReceiveChannel<T>>.zip(bufferSize: Int = 100): ReceiveChannel<List<T>> = produce {
    do {
        val indexToCandles = map {
            it.take(bufferSize).toList()
        }
        val chunk: List<List<T>> = indexToCandles.zip()
        chunk.forEach {
            send(it)
        }
    } while (chunk.size == bufferSize)
}

private fun <T> List<List<T>>.zip(): List<List<T>> {
    val newSize = minBy { it.size }!!.size
    return (0 until newSize).map { i -> this.map { it[i] } }
}

@Serializable
data class BinanceTradeConfig(val market: String)

class BinanceTradeSource(
        override val config: BinanceTradeConfig,
        var currentTime: Instant,
        private val getTrades: (startId: BinanceAggTradeId, beforeTime: Instant) -> ReceiveChannel<TradeItem>
) : SyncFileArray.Source<BinanceTradeConfig, BinanceAggTradeId, Trade> {
    override fun getNew(lastId: BinanceAggTradeId?): ReceiveChannel<TradeItem> {
        return getTrades(lastId ?: 0, currentTime)
    }
}

class MomentSource(
        override val config: MomentConfig,
        var currentTime: Instant,
        private val getTrades: (coin: String, startId: TradeId) -> ReceiveChannel<TradeItem>
) : SyncFileArray.Source<MomentConfig, MomentId, Moment> {
    override fun getNew(lastId: MomentId?): ReceiveChannel<MomentItem> {
        return config.coins
                .mapIndexed { i, it ->
                    val aggTradeId: Long = if (lastId != null) {
                        lastId.candles[i].firstTradeId
                    } else {
                        0L
                    }
                    getTrades(it, aggTradeId).candles(config.startTime, currentTime, config.period)
                }
                .moments()
    }
}

class ArraySource<out CONFIG : Any, out ITEM>(
        override val config: CONFIG,
        val array: SuspendArray<ITEM>,
        private val bufferSize: Int = 100
) : SyncFileArray.Source<CONFIG, Long, ITEM> {
    override fun getNew(lastId: Long?): ReceiveChannel<SyncFileArray.Source.Item<Long, ITEM>> {
        fun item(index: Long, item: ITEM) = SyncFileArray.Source.Item(index, index, item)

        val startIndex = (lastId ?: -1) + 1
        return (startIndex until array.size)
                .rangeChunked(bufferSize.toLong())
                .asReceiveChannel()
                .map { range ->
                    val items = array.get(range)
                    range.zip(items, ::item)
                }
                .flatten()
    }
}

private fun LongRange.rangeChunked(size: Long): List<LongRange> {
    val ranges = ArrayList<LongRange>()
    for (st in start until endInclusive step size) {
        val nd = Math.min(endInclusive, st + size)
        ranges.add(LongRange(st, nd))
    }
    return ranges
}

class SyncFileArray<in CONFIG : Any, ITEMID : Any, ITEM>(
        file: Path,
        configSerializer: KSerializer<CONFIG>,
        idSerializer: KSerializer<ITEMID>,
        itemSerializer: FixedSerializer<ITEM>,
        private val bufferSize: Int = 100
) : SuspendArray<ITEM> {
    private val configStore = AtomicFileStore(file.appendToFileName(".config"), configSerializer)
    private val lastIdStore = AtomicFileStore(file.appendToFileName(".lastId"), idSerializer)
    private val fileArray = FileArray(file.appendToFileName(".array"), itemSerializer)

    override val size: Long get() = fileArray.size
    override suspend fun get(range: LongRange): List<ITEM> = fileArray.get(range)

    suspend fun syncWith(source: Source<CONFIG, ITEMID, ITEM>) {
        val config = configStore.readOrNull()
        if (config != source.config) {
            lastIdStore.remove()
            fileArray.clear()
            configStore.write(source.config)
        }

        val lastId = lastIdStore.readOrNull()

        var isFirst = true
        source.getNew(lastId).chunked(bufferSize).consumeEach {
            val index = it.last().index
            val id = it.last().id
            val items = it.map { it.value }

            if (isFirst) {
                fileArray.reduceSize(index)
                isFirst = false
            }

            require(index == fileArray.size + items.size - 1)
            fileArray.append(items)
            lastIdStore.write(id)
        }
    }

    interface Source<out CONFIG : Any, ITEMID : Any, out ITEM> {
        val config: CONFIG
        fun getNew(lastId: ITEMID?): ReceiveChannel<Item<ITEMID, ITEM>>
        data class Item<out ID : Any, out ITEM>(val index: Long, val id: ID, val value: ITEM)
    }
}

private class MomentSerializer(size: Int) : FixedSerializer<Moment> {
    val listSerializer = FixedListSerializer(size, CandleSerializer())
    override val itemBytes: Int = listSerializer.itemBytes
    override fun serialize(item: Moment, data: ByteBuffer) = listSerializer.serialize(item.coinIndexToCandle, data)
    override fun deserialize(data: ByteBuffer): Moment = Moment(listSerializer.deserialize(data))
}

private class TradeSerializer : FixedSerializer<Trade> {
    override val itemBytes: Int = 3 * 8

    override fun serialize(item: Trade, data: ByteBuffer) {
        data.putLong(item.time.toEpochMilli())
        data.putDouble(item.amount)
        data.putDouble(item.price)
    }

    override fun deserialize(data: ByteBuffer): Trade = Trade(
            Instant.ofEpochMilli(data.long),
            data.double,
            data.double
    )
}

private class CandleSerializer : FixedSerializer<Candle> {
    override val itemBytes: Int = 3 * 8

    override fun serialize(item: Candle, data: ByteBuffer) {
        data.putDouble(item.close)
        data.putDouble(item.high)
        data.putDouble(item.low)
    }

    override fun deserialize(data: ByteBuffer): Candle = Candle(
            data.double,
            data.double,
            data.double
    )
}