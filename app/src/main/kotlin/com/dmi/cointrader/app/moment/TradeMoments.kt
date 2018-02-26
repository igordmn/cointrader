//package com.dmi.cointrader.app.moment
//
//import com.dmi.cointrader.app.trade.Trade
//import com.dmi.util.concurrent.flatten
//import com.dmi.util.io.AtomicFileDataStore
//import com.dmi.util.io.AtomicFileStore
//import com.dmi.util.io.FileFixedArray
//import com.dmi.util.io.appendToFileName
//import com.google.common.hash.Hashing
//import com.sun.org.apache.xpath.internal.operations.Bool
//import exchange.ReversedMarketHistory
//import exchange.binance.BinanceConstants
//import kotlinx.coroutines.experimental.channels.*
//import kotlinx.serialization.Serializable
//import java.io.ByteArrayOutputStream
//import java.nio.ByteBuffer
//import java.nio.file.Path
//import java.nio.file.Paths
//import java.time.Duration
//import java.time.LocalDateTime
//import java.time.ZoneOffset
//import java.util.*
//
//
//data class MarketInfo(val coin: String, val name: String, val isReversed: Boolean)
//
//fun marketInfo(coin: String): MarketInfo {
//    val mainCoin = "BTC"
//    val constants = BinanceConstants()
//
//    val name = constants.marketName(coin, mainCoin)
//    val reversedName = constants.marketName(mainCoin, coin)
//
//    return when {
//        name != null -> MarketInfo(coin, name, false)
//        reversedName != null -> MarketInfo(coin, reversedName, true)
//        else -> throw UnsupportedOperationException()
//    }
//}
//
//fun main(args: Array<String>) {
//    val startTime = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3"))
//    val period = Duration.ofMinutes(5)
//    val coins: List<String> = listOf(
//            "USDT", "ETH", "TRX", "XRP", "LTC", "ETC", "ICX"
//    )
//
//    val candles = coins.map { coin ->
//        val marketInfo = marketInfo(coin)
//        val market = marketInfo.name
//        val isReversed = marketInfo.isReversed
//
//        fun reverseIfNeeded(x) = if (isReversed) x.map(Trade::reverse) else x
//
//        val trades = ztrades(market, timeStore)
//                .cacheTo(Paths.get("D:/yy/trades/$market"))    // lastAggTradeId
//                .reverseIfNeeded()
//
//        zcandles(startTime, period, trades)
//    }
//    val moments = zzip(candles).map(::TradeMoment).cacheTo(Paths.get("D:/yy/moments"))  // coins, startTime, period, candle algorithm, coin_to_tradeIndex
//
//    timeStore.load()
//    moments.load()
//}
//
//
//interface ComputedFileArray<R> {
//    val id: ByteArray
//    val size: Long
//    suspend fun compute()
//    suspend fun get(range: LongRange): List<R>
//}
//
//class TransformFileArray<T, R>(
//        file: Path,
//        serializer: FileFixedArray.Serializer<R>,
//        private val original: ComputedFileArray<T>,
//        private val transformId: ByteArray,
//        private val transform: suspend (ReceiveChannel<ItemInfo<T>>) -> ReceiveChannel<ItemInfo<R>>
//) : ComputedFileArray<R> {
//    override val id: ByteArray = hash(listOf(transformId, original.id))
//
//    private val idStore = AtomicFileDataStore(file.appendToFileName(".id"))
//    private val metaStore = AtomicFileStore(file.appendToFileName(".meta"), Meta.serializer())
//    private val fileArray = FileFixedArray(file.appendToFileName(".array"), serializer)
//
//    override suspend fun compute() {
//        val windowSize = 1000L
//
//        original.compute()
//
//        val meta: Meta
//
//        if (idStore.exists()) {
//            val storedId = idStore.read()
//            if (!Arrays.equals(storedId, id)) {
//                fileArray.clear()
//                meta = Meta(0, 0)
//                metaStore.write(meta)
//                idStore.write(id)
//            } else {
//                meta = metaStore.read()
//                fileArray.reduceSize(meta.thisIndex)
//            }
//        } else {
//            fileArray.clear()
//            meta = Meta(0, 0)
//            metaStore.write(meta)
//            idStore.write(id)
//        }
//
//        (meta.originalLastIndex until original.size)
//                .rangeChunked(windowSize)
//                .asReceiveChannel()
//                .map { range ->
//                    original.get(range).mapIndexed { i, it ->
//                        ItemInfo(range.start + i, it)
//                    }
//                }
//                .flatten()
//                .apply { transform(this) }
//                .chunked(windowSize)
//                .forEach {
//                    metaStore.write(Meta(it.last().originalLastIndex, meta.thisIndex + it.size))
//                    fileArray.append(it)
//                }
//
//    }
//
//    override val size: Long = fileArray.size
//    override suspend fun get(range: LongRange): List<R> = fileArray.get(range)
//
//    @Serializable
//    data class Meta(val originalLastIndex: Long, val thisIndex: Long)
//
//    class ItemInfo<out T>(val originalIndex: Long, val item: T)
//}
//
//class CombinedFileArray<T>(
//        file: Path,
//        serializer: FileFixedArray.Serializer<T>,
//        private val params: List<ComputedFileArray<T>>
//) : ComputedFileArray<List<T>> {
//    override val id: ByteArray = hash(params.map(ComputedFileArray<T>::id))
//
//    private val idStore = AtomicFileDataStore(file.appendToFileName(".id"))
//    private val fileArray = FileFixedArray(file.appendToFileName(".array"), ListSerializer(params.size, serializer))
//
//    init {
//        require(params.isNotEmpty())
//    }
//
//    override suspend fun compute() {
//        val windowSize = 1000L
//
//        params.forEach {
//            it.compute()
//        }
//
//        if (idStore.exists()) {
//            val storedId = idStore.read()
//            if (!Arrays.equals(storedId, id)) {
//                fileArray.clear()
//                idStore.write(id)
//            }
//        } else {
//            fileArray.clear()
//            idStore.write(id)
//        }
//
//        val minSize = params.minBy(ComputedFileArray<T>::size)!!.size
//        val storedSize = fileArray.size
//
//        (storedSize until minSize).rangeChunked(windowSize).forEach { range ->
//            val itemToParams = params.map { it.get(range) }.transpose()
//            fileArray.append(itemToParams)
//        }
//    }
//
//    override val size: Long = fileArray.size
//    override suspend fun get(range: LongRange): List<List<T>> = fileArray.get(range)
//}
//
//private fun hash(arrays: List<ByteArray>): ByteArray {
//    val outputStream = ByteArrayOutputStream()
//    arrays.forEach {
//        outputStream.write(it)
//    }
//    return Hashing.murmur3_128().hashBytes(outputStream.toByteArray()).asBytes()
//}
//
//private fun LongRange.rangeChunked(size: Long): List<LongRange> {
//    val ranges = ArrayList<LongRange>()
//    for (st in start until endInclusive step size) {
//        val nd = Math.min(endInclusive, st + size)
//        ranges.add(LongRange(st, nd))
//    }
//    return ranges
//}
//
//private fun <T> List<List<T>>.transpose(): List<List<T>> {
//    val newSize = first().size
//    return (0 until newSize).map { i -> this.map { it[i] } }
//}
//
//private class ListSerializer<T>(
//        private val size: Int,
//        private val original: FileFixedArray.Serializer<T>
//) : FileFixedArray.Serializer<List<T>> {
//    override val itemBytes: Int = original.itemBytes * size
//
//    override fun serialize(item: List<T>, data: ByteBuffer) = item.forEach {
//        original.serialize(it, data)
//    }
//
//    override fun deserialize(data: ByteBuffer): List<T> = (1..size).map {
//        original.deserialize(data)
//    }
//}