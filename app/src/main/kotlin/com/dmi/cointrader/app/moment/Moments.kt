package com.dmi.cointrader.app.moment

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.TradesCandle
import com.dmi.cointrader.app.candle.candles
import com.dmi.cointrader.app.trade.Trade
import com.dmi.util.collection.SuspendArray
import com.dmi.util.concurrent.zip
import com.dmi.util.io.IdentitySource
import com.dmi.util.io.Indexed
import com.dmi.util.io.NumIdIndex
import com.dmi.util.io.SyncFileArray
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.mapIndexed
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

@Serializable
data class MomentConfig(val startTime: Instant, val period: Duration, val coins: List<String>)

@Serializable
data class CandleId(val firstTradeIndex: Long)

@Serializable
data class MomentId(val candles: List<CandleId>)

typealias CandleIndex = NumIdIndex<CandleId>
typealias MomentIndex = NumIdIndex<MomentId>

typealias CandleItem = Indexed<CandleIndex, Candle>
typealias MomentItem = Indexed<MomentIndex, Moment>

class MomentSource(
        override val config: MomentConfig,
        var currentTime: Instant,
        private val coinIndexToTrades: List<SuspendArray<Trade>>
) : IdentitySource<MomentConfig, MomentIndex, Moment> {
    override fun after(lastIndex: MomentIndex?): ReceiveChannel<MomentItem> {
        return config.coins.indices
                .map { i ->
                    candlesAfter(i, lastIndex)
                }
                .moments()
    }

    private fun candlesAfter(coinIndex: Int, lastIndex: MomentIndex?): ReceiveChannel<CandleItem> {
        val startNum: Long
        val startTradeIndex: Long
        if (lastIndex != null) {
            startNum = lastIndex.num
            startTradeIndex = lastIndex.id.candles[coinIndex].firstTradeIndex
        } else {
            startNum = 0
            startTradeIndex = 0
        }
        val trades = coinIndexToTrades[coinIndex]
        return trades
                .channelIndexed(startTradeIndex)
                .candles(config.startTime, currentTime, config.period)
                .toItems(startNum)
    }

    private fun ReceiveChannel<TradesCandle<Long>>.toItems(startNum: Long): ReceiveChannel<CandleItem> = mapIndexed { i, it ->
        CandleItem(CandleIndex(startNum + i, CandleId(it.firstTradeIndex)), it.candle)
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
        return MomentItem(NumIdIndex(index, id), value)
    }
}

fun momentArray(path: Path, config: MomentConfig) = SyncFileArray(
        path,
        MomentConfig.serializer(),
        TODO() as KSerializer<MomentIndex>,
        MomentFixedSerializer(config.coins.size)
)