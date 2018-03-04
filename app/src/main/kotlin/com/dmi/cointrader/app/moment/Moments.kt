package com.dmi.cointrader.app.moment

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.TradesCandle
import com.dmi.cointrader.app.candle.candleNum
import com.dmi.cointrader.app.candle.candles
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.tradesFileTable
import com.dmi.util.collection.Row
import com.dmi.util.collection.SuspendArray
import com.dmi.util.collection.Table
import com.dmi.util.concurrent.map
import com.dmi.util.concurrent.zip
import com.dmi.util.io.SyncFileTable
import com.dmi.util.io.syncFileTable
import exchange.binance.BinanceConstants
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.withIndex
import kotlinx.serialization.Serializable
import main.test.Config
import java.nio.file.Path
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
        private val config: MomentsConfig,
        private val coinIndexToTrades: List<SuspendArray<Trade>>,
        var currentTime: Instant
) : Table<MomentId, Moment> {
    override fun rowsAfter(id: MomentId?): ReceiveChannel<MomentRow> {
        require(currentTime >= config.startTime)

        val firstNum = if (id != null) id.num + 1 else 0L
        val lastNum = candleNum(config.startTime, config.period, currentTime)

        val tradeStartIndices = if (id != null) {
            id.candles.map(CandleId::lastTradeIndex)
        } else {
            config.coins.indices.map { 0L }
        }

        fun TradesCandle<Long>.toRow() = CandleRow(CandleId(lastTradeIndex), candle)

        fun candles(i: Int) = coinIndexToTrades[i]
                .channelIndexed(tradeStartIndices[i])
                .candles(config.startTime, config.period, firstNum..lastNum)
                .map(TradesCandle<Long>::toRow)

        fun moment(candles: IndexedValue<List<CandleRow>>): MomentRow {
            val num = firstNum + candles.index
            val candleIds = candles.value.map { it.id }
            val candleValues = candles.value.map { it.value }
            val value = Moment(candleValues)
            return MomentRow(MomentId(num, candleIds), value)
        }

        return config.coins.indices
                .map(::candles)
                .zip()
                .withIndex()
                .map(::moment)
    }
}

suspend fun momentsFileTable(
        path: Path,
        config: MomentsConfig
) = syncFileTable(
        path,
        MomentsConfig.serializer(),
        MomentId.serializer(),
        MomentFixedSerializer(config.coins.size),
        reloadCount = 10
)

interface Moments : SuspendArray<Moment> {
    suspend fun sync(currentTime: Instant)
}

suspend fun moments(
        config: Config,
        api: BinanceAPI,
        constant: BinanceConstants,
        currentTime: Instant
): Moments {
    val coinToTrades = config.altCoins.map { coin ->
        val marketInfo = constant.marketInfo(coin, config.mainCoin)
        tradesFileTable(api, marketInfo, currentTime)
    }

    val momentsConfig = MomentsConfig(config.trainStartTime, config.period, config.altCoins)
    val array = momentsFileTable(Paths.get("data/cache/binance/moments"), momentsConfig)
    val source = MomentsSource(momentsConfig, currentTime, coinToTrades)
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