package com.dmi.cointrader.app.moment

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.TradesCandle
import com.dmi.cointrader.app.candle.candleNum
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.trades
import com.dmi.util.atom.ReadAtom
import com.dmi.util.collection.Row
import com.dmi.util.collection.Table
import com.dmi.util.concurrent.buildChannel
import com.dmi.util.io.SyncFileTable
import com.dmi.util.io.SyncTable
import com.dmi.util.io.syncFileTable
import exchange.binance.BinanceConstants
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.serialization.Serializable
import main.test.Config
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

@Serializable
data class MomentsConfig(val startTime: Instant, val period: Duration, val coins: List<String>)

@Serializable
data class CandleId(val lastTradeIndex: Long)

@Serializable
data class MomentId(val num: Long, val candles: List<CandleId>)

typealias CandleRow = Row<CandleId, Candle>
typealias MomentRow = Row<MomentId, Moment>

class TradeMoments(
        private val startTime: Instant,
        private val period: Duration,
        private val trades: List<Table<Long, Trade>>,
        private val currentTime: ReadAtom<Instant>
) : Table<MomentId, Moment> {
    override fun rowsAfter(id: MomentId?): ReceiveChannel<MomentRow> = buildChannel {
        val currentTime = currentTime()
        val indices = trades.indices

        require(currentTime >= startTime)

        val firstNum = if (id != null) id.num + 1 else 0L
        val lastNum = candleNum(startTime, period, currentTime)
        val tradeStartIndices = id?.candles?.map(CandleId::lastTradeIndex) ?: indices.map { 0L }

        fun TradesCandle<Long>.toRow() = CandleRow(CandleId(lastTradeIndex), candle)

        fun candles(i: Int) = trades[i]
                .rowsAfter(tradeStartIndices[i])
                .candles(startTime, period, firstNum..lastNum)
                .map(TradesCandle<Long>::toRow)

        fun moment(candles: IndexedValue<List<CandleRow>>): MomentRow {
            val num = firstNum + candles.index
            val candleIds = candles.value.map { it.id }
            val candleValues = candles.value.map { it.value }
            val value = Moment(candleValues)
            return MomentRow(MomentId(num, candleIds), value)
        }

        indices
                .map(::candles)
                .zip()
                .withIndex()
                .map(::moment)
    }
}

suspend fun moments(
        config: Config,
        api: BinanceAPI,
        constant: BinanceConstants,
        currentTime: ReadAtom<Instant>
): SyncTable<Moment> {
    val coinToTrades = config.altCoins.map { coin ->
        val marketInfo = constant.marketInfo(coin, config.mainCoin)
        trades(api, currentTime, marketInfo)
    }

    val momentsConfig = MomentsConfig(config.trainStartTime, config.period, config.altCoins)
    val path = Paths.get("data/cache/binance/moments")
    val source = TradeMoments(config.trainStartTime, config.period, coinToTrades)
    val table = syncFileTable(
            path,
            MomentsConfig.serializer(),
            MomentId.serializer(),
            MomentFixedSerializer(config.altCoins.size),
            config,
            source,
            reloadCount = 10
    )
    array.syncWith(source)

    return object : Moments {
        override suspend fun sync(currentTime: Instant) {
            source.currentTime = currentTime
            array.syncWith(source)
        }

        override val size: Long = array.size
        suspend override fun get(range: LongRange): List<Moment> = array.get(range)
    }
}