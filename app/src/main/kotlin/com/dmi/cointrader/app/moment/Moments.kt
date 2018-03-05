package com.dmi.cointrader.app.moment

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.TradesCandle
import com.dmi.cointrader.app.candle.candleNum
import com.dmi.cointrader.app.candle.candles
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.trades
import com.dmi.util.atom.ReadAtom
import com.dmi.util.collection.Row
import com.dmi.util.collection.Table
import com.dmi.util.concurrent.buildChannel
import com.dmi.util.concurrent.map
import com.dmi.util.concurrent.zip
import com.dmi.util.io.SyncTable
import com.dmi.util.io.syncFileTable
import exchange.binance.BinanceConstants
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.withIndex
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
        val tradesAfter = id?.candles?.map { it.lastTradeIndex - 1 } ?: indices.map { null }

        fun TradesCandle<Long>.toRow() = CandleRow(CandleId(lastTradeIndex), candle)

        fun candles(i: Int) = trades[i]
                .rowsAfter(tradesAfter[i])
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

    return syncFileTable(
            Paths.get("data/cache/binance/moments"),
            MomentsConfig.serializer(),
            MomentId.serializer(),
            MomentFixedSerializer(config.altCoins.size),
            MomentsConfig(config.trainStartTime, config.period, config.altCoins),
            TradeMoments(config.trainStartTime, config.period, coinToTrades, currentTime),
            reloadCount = 10
    )
}