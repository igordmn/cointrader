package com.dmi.cointrader.app.moment

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.TradesCandle
import com.dmi.cointrader.app.candle.candles
import com.dmi.cointrader.app.candle.periodNum
import com.dmi.cointrader.app.trade.Trade
import com.dmi.util.collection.Indexed
import com.dmi.util.collection.NumIdIndex
import com.dmi.util.collection.SuspendArray
import com.dmi.util.collection.openRight
import com.dmi.util.concurrent.zip
import com.dmi.util.io.SyncSource
import com.dmi.util.io.SyncFileArray
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

@Serializable
data class MomentsConfig(val startTime: Instant, val period: Duration, val coins: List<String>, val reloadLastCount: Int)

@Serializable
data class CandleId(val lastTradeIndex: Long)

@Serializable
data class MomentId(val candles: List<CandleId>)

typealias CandleIndex = NumIdIndex<CandleId>
typealias MomentIndex = NumIdIndex<MomentId>

typealias CandleItem = Indexed<CandleIndex, Candle>
typealias MomentItem = Indexed<MomentIndex, Moment>

class MomentSource(
        override val config: MomentsConfig,
        var currentTime: Instant,
        private val coinIndexToTrades: List<SuspendArray<Trade>>
) : SyncSource<MomentsConfig, MomentIndex, Moment> {
    override fun newItems(lastIndex: MomentIndex?): ReceiveChannel<MomentItem> {
        require(currentTime >= config.startTime)

        val firstNum = if (lastIndex != null) lastIndex.num + 1 else 0L
        val lastNum = periodNum(config.startTime, config.period, currentTime)

        val tradeStartIndices = if (lastIndex != null) {
            lastIndex.id.candles.map(CandleId::lastTradeIndex)
        } else {
            config.coins.indices.map { 0L }
        }

        return config.coins.indices
                .map { i ->
                    coinIndexToTrades[i]
                            .channelIndexed(tradeStartIndices[i])
                            .candles(config.startTime, config.period, firstNum..lastNum)
                            .toItems(lastNum)
                }
                .moments()
    }

    private fun ReceiveChannel<TradesCandle<Long>>.toItems(lastNum: Long): ReceiveChannel<CandleItem> {
        var previousIndex: CandleIndex? = null
        return map {
            val index = if (previousIndex == null || it.periodNum <= lastNum - config.reloadLastCount) {
                CandleIndex(it.periodNum, CandleId(it.lastTradeIndex))
            } else {
                previousIndex!!
            }
            previousIndex = index
            CandleItem(index, it.candle)
        }
    }

    private fun List<ReceiveChannel<CandleItem>>.moments(): ReceiveChannel<MomentItem> = zip().map { moment(it) }

    private fun moment(candles: List<CandleItem>): MomentItem {
        require(candles.isNotEmpty())
        val num = candles.first().index.num
        candles.forEach {
            require(it.index.num == num)
        }
        val candleIds = candles.map { it.index.id }
        val candleValues = candles.map { it.value }
        val id = MomentId(candleIds)
        val value = Moment(candleValues)
        return MomentItem(NumIdIndex(num, id), value)
    }
}

fun momentArray(path: Path, config: MomentsConfig) = SyncFileArray(
        path,
        MomentsConfig.serializer(),
        TODO() as KSerializer<MomentIndex>,
        MomentFixedSerializer(config.coins.size)
)