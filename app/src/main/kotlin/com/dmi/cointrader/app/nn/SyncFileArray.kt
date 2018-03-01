package com.dmi.cointrader.app.nn

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.moment.MomentFixedSerializer
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.TradeFixedSerializer
import com.dmi.util.collection.SuspendArray
import com.dmi.util.collection.rangeChunked
import com.dmi.util.concurrent.flatten
import com.dmi.util.concurrent.zip
import com.dmi.util.io.IdentitySource
import com.dmi.util.io.SyncFileArray
import exchange.binance.BinanceConstants
import exchange.binance.api.binanceAPI
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
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
        val startTime = LocalDateTime.of(2017, 8, 1, 0, 0, 0).toInstant(ZoneOffset.of("+3"))
        val period = Duration.ofMinutes(5)
        val coins: List<String> = listOf(
                "USDT", "ETH", "TRX", "XRP", "LTC", "ETC", "ICX"
        )

        val momentConfig = MomentConfig(startTime, period, coins)
        val currentTime = Instant.ofEpochMilli(api.serverTime().serverTime)

        val coinToTrades = coins.map { coin ->
            val market = marketInfo(coin)
            val binanceTrades = SyncFileArray(
                    Paths.get("D:/yy/trades/$market"),
                    BinanceTradeConfig.serializer(),
                    LongSerializer,
                    TradeFixedSerializer()
            )

            fun getTrades(startIndex: BinanceTradeIndex, beforeTime: Instant): ReceiveChannel<TradeItem> {
                return binanceTrades(api, market.name, startIndex.id, beforeTime).mapIndexed { i, it ->
                    IdentitySource.Item(IdentitySource.Index(startIndex.num + i, it.aggregatedId), it.trade)
                }
            }

            val binanceTradesSource = BinanceTradeSource(BinanceTradeConfig(market.name), currentTime, ::getTrades)
            binanceTrades.syncWith(binanceTradesSource)

            // TODO reverse
            ArraySource(Unit, binanceTrades)
        }

        fun getTrades(coinIndex: Int, startId: TradeId): ReceiveChannel<TradeItem> {
            return coinToTrades[coinIndex].getNew()
        }

        val moments = SyncFileArray(
                Paths.get("D:/yy/moments"),
                MomentConfig.serializer(),
                MomentId.serializer(),
                MomentFixedSerializer(coins.size)
        )

        val momentsSource = MomentSource(momentConfig, currentTime, ::getTrades)
        moments.syncWith(momentsSource)
    }
}


@Serializable
data class MomentConfig(val startTime: Instant, val period: Duration, val coins: List<String>)

@Serializable
data class CandleId(val firstTradeId: Long)

@Serializable
data class MomentId(val candles: List<CandleId>)

typealias BinanceTradeId = Long
typealias TradeId = Long
typealias BinanceTradeIndex = IdentitySource.Index<BinanceTradeId>
typealias TradeIndex = IdentitySource.Index<TradeId>
typealias MomentIndex = IdentitySource.Index<MomentId>
typealias TradeItem = IdentitySource.Item<TradeId, Trade>
typealias CandleItem = IdentitySource.Item<CandleId, Candle>
typealias MomentItem = IdentitySource.Item<MomentId, Moment>


fun periodIndex(startTime: Instant, period: Duration, time: Instant): Long {
    return Duration.between(time, startTime).toMillis() / period.toMillis()
}


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
            IdentitySource.Index(
                    periodIndex.apply { require(this >= 0) },
                    CandleId(trades.first().index.id)
            ),
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
        val startIndex = if (last != null) last!!.index.num + 1 else 0
        require(it.index.num >= startIndex)

        for (i in startIndex until it.index.num) {
            send(CandleItem(IdentitySource.Index(i, it.index.id), it.value))
        }

        send(it)

        last = it
    }

    last?.let {
        for (i in it.index.num until endIndex) {
            send(CandleItem(IdentitySource.Index(i, it.index.id), it.value))
        }
    }
}

private fun List<ReceiveChannel<CandleItem>>.moments(): ReceiveChannel<MomentItem> = zip().map { moment(it) }

private fun moment(candles: List<CandleItem>): MomentItem {
    require(candles.isNotEmpty())
    val index = candles.first().index.num
    candles.forEach {
        require(it.index.num == index)
    }
    val candleIds = candles.map { it.index.id }
    val candleValues = candles.map { it.value }
    val id = MomentId(candleIds)
    val value = Moment(candleValues)
    return MomentItem(IdentitySource.Index(index, id), value)
}

@Serializable
data class BinanceTradeConfig(val market: String)

class BinanceTradeSource(
        override val config: BinanceTradeConfig,
        var currentTime: Instant,
        private val getTrades: (startIndex: BinanceTradeIndex, beforeTime: Instant) -> ReceiveChannel<TradeItem>
) : IdentitySource<BinanceTradeConfig, BinanceTradeId, Trade> {
    override fun getNew(lastIndex: BinanceTradeIndex?): ReceiveChannel<TradeItem> {
        val startIndex = lastIndex ?: BinanceTradeIndex(0, 0)
        return getTrades(startIndex, currentTime)
    }
}

class MomentSource(
        override val config: MomentConfig,
        var currentTime: Instant,
        private val getTrades: (coinIndex: Int, startIndex: TradeIndex) -> ReceiveChannel<TradeItem>
) : IdentitySource<MomentConfig, MomentId, Moment> {
    override fun getNew(lastIndex: MomentIndex?): ReceiveChannel<MomentItem> {
        return config.coins.indices
                .map { i ->
                    val startIndex = if (lastIndex != null) {
                        IdentitySource.Index(
                                lastIndex.num,
                                lastIndex.id.candles[i].firstTradeId
                        )
                    } else {
                        null
                    }
                    getTrades(i, startIndex).candles(config.startTime, currentTime, config.period)
                }
                .moments()
    }
}

class ArraySource<out CONFIG : Any, out ITEM>(
        override val config: CONFIG,
        val array: SuspendArray<ITEM>,
        private val bufferSize: Int = 100
) : IdentitySource<CONFIG, Long, ITEM> {
    override fun getNew(lastIndex: IdentitySource.Index<Long>?): ReceiveChannel<IdentitySource.Item<Long, ITEM>> {
        fun item(index: Long, item: ITEM) = IdentitySource.Item(IdentitySource.Index(index, index), item)

        val startIndex = (lastIndex?.num ?: -1) + 1
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