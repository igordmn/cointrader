package com.dmi.cointrader.app.moment

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.TradesCandle
import com.dmi.cointrader.app.candle.periodNum
import com.dmi.cointrader.app.candle.candles
import com.dmi.cointrader.app.trade.Trade
import com.dmi.util.atom.ReadAtom
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.*
import com.dmi.util.io.RestorableSource
import com.dmi.util.io.SyncList
import com.dmi.util.io.syncFileList
import com.dmi.util.lang.DurationSerializer
import com.dmi.util.lang.InstantSerializer
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.serialization.Serializable
import com.dmi.cointrader.main.Config
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

@Serializable
data class MomentsConfig(
        @Serializable(with=InstantSerializer::class) val startTime: Instant,
        @Serializable(with=DurationSerializer::class) val period: Duration,
        val coins: List<String>
)

@Serializable
data class CandleState(val lastTradeIndex: Long)

@Serializable
data class MomentState(val num: Long, val candles: List<CandleState>)

typealias CandleItem = RestorableSource.Item<CandleState, Candle>
typealias MomentItem = RestorableSource.Item<MomentState, Moment>

class TradeMoments(
        private val startTime: Instant,
        private val period: Duration,
        private val trades: List<SuspendList<Trade>>,
        private val currentTime: ReadAtom<Instant>
) : RestorableSource<MomentState, Moment> {
    override fun restore(state: MomentState?): ReceiveChannel<MomentItem> = buildChannel {
        val currentTime = currentTime()
        val indices = trades.indices

        require(currentTime >= startTime)

        val firstNum = if (state != null) state.num + 1 else 0L
        val lastNum = periodNum(startTime, period, currentTime)
        val tradeStartIndices = state?.candles?.map(CandleState::lastTradeIndex) ?: indices.map { 0L }

        fun TradesCandle<Long>.toItem() = CandleItem(CandleState(lastTradeIndex), candle)

        fun candles(i: Int) = trades[i]
                .channel(tradeStartIndices[i])
                .withLongIndex(tradeStartIndices[i])
                .candles(startTime, period, firstNum..lastNum)
                .map(TradesCandle<Long>::toItem)

        fun moment(candles: LongIndexed<List<CandleItem>>): MomentItem {
            val num = candles.index
            val candleStates = candles.value.map { it.state }
            val candleValues = candles.value.map { it.value }
            val value = Moment(candleValues)
            return MomentItem(MomentState(num, candleStates), value)
        }

        indices
                .map(::candles)
                .zip()
                .withLongIndex(firstNum)
                .map(::moment)
    }
}

suspend fun cachedMoments(
        config: Config,
        coinToTrades: List<SuspendList<Trade>>,
        currentTime: ReadAtom<Instant>
): SyncList<Moment> {
    val path = Paths.get("old/data/cache/binance/moments")
    Files.createDirectories(path.parent)
    return cachedMoments(config.startTime, config.period, config.altCoins, path, coinToTrades, currentTime)
}

suspend fun cachedMoments(
        startTime: Instant,
        period: Duration,
        altCoins: List<String>,
        path: Path,
        coinToTrades: List<SuspendList<Trade>>,
        currentTime: ReadAtom<Instant>,
        reloadCount: Int = 10
): SyncList<Moment> {
    return syncFileList(
            path,
            MomentsConfig.serializer(),
            MomentState.serializer(),
            MomentFixedSerializer(altCoins.size),
            MomentsConfig(startTime, period, altCoins),
            TradeMoments(startTime, period, coinToTrades, currentTime),
            reloadCount = reloadCount
    )
}