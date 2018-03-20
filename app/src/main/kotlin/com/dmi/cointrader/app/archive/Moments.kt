package com.dmi.cointrader.app.archive

import com.dmi.cointrader.app.candle.*
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.*
import com.dmi.util.io.RestorableSource
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class MomentsConfig(
        val periods: Periods,
        val assets: List<String>
)

@Serializable
data class CandleState(val lastTradeIndex: Long)

@Serializable
data class MomentState(val num: Long, val candles: List<CandleState>)

typealias CandleItem = RestorableSource.Item<CandleState, Candle>
typealias MomentItem = RestorableSource.Item<MomentState, Moment>

class TradeMoments(
        private val periods: Periods,
        private val trades: List<SuspendList<Trade>>,
        private val currentTime: Instant
) : RestorableSource<MomentState, Moment> {
    init {
        require(currentTime >= periods.start)
    }

    override fun restore(state: MomentState?): ReceiveChannel<MomentItem> = buildChannel {
        val indices = trades.indices
        val firstNum = if (state != null) state.num + 1 else 0L
        val lastNum = periods.of(currentTime).num
        val tradeStartIndices = state?.candles?.map(CandleState::lastTradeIndex) ?: indices.map { 0L }

        fun TradesCandle<Long>.toItem() = CandleItem(CandleState(lastTradeIndex), candle)

        fun candles(i: Int) = trades[i]
                .channel(tradeStartIndices[i])
                .withLongIndex(tradeStartIndices[i])
                .candles(periods, firstNum..lastNum)
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