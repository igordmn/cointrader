//package com.dmi.cointrader.app.moment
//
//import com.dmi.cointrader.app.candle.Candle
//import com.dmi.cointrader.app.candle.TradesCandle
//import com.dmi.cointrader.app.candle.candles
//import com.dmi.cointrader.app.trade.Trade
//import com.dmi.util.collection.SuspendArray
//import com.dmi.util.concurrent.zip
//import com.dmi.util.io.IdentitySource
//import com.dmi.util.io.Indexed
//import com.dmi.util.io.NumIdIndex
//import com.dmi.util.io.SyncFileArray
//import kotlinx.coroutines.experimental.channels.ReceiveChannel
//import kotlinx.coroutines.experimental.channels.map
//import kotlinx.coroutines.experimental.channels.mapIndexed
//import kotlinx.coroutines.experimental.channels.takeWhile
//import kotlinx.serialization.KSerializer
//import kotlinx.serialization.Serializable
//import java.nio.file.Path
//import java.time.Duration
//import java.time.Instant
//
//@Serializable
//data class MomentsConfig(val startTime: Instant, val period: Duration, val coins: List<String>, val reloadLastCount: Int)
//
//@Serializable
//data class CandleId(val firstTradeIndex: Long)
//
//@Serializable
//data class MomentId(val candles: List<CandleId>)
//
//typealias CandleIndex = NumIdIndex<CandleId>
//typealias MomentIndex = NumIdIndex<MomentId>
//
//typealias CandleItem = Indexed<CandleIndex, Candle>
//typealias MomentItem = Indexed<MomentIndex, Moment>
//
//class MomentSource(
//        override val config: MomentsConfig,
//        var currentTime: Instant,
//        private val coinIndexToCandles: List<SuspendArray<Candle>>
//) : IdentitySource<MomentsConfig, MomentIndex, Moment> {
//    override fun newItems(lastIndex: MomentIndex?): ReceiveChannel<MomentItem> {
//        return config.coins.indices
//                .map { i ->
//                    candlesAfter(i, lastIndex)
//                }
//                .moments()
//    }
//
//    private fun candlesAfter(coinIndex: Int, lastIndex: MomentIndex?): ReceiveChannel<CandleItem> {
//        val startNum: Long
//        val firstTradeIndex: Long
//        if (lastIndex != null) {
//            startNum = lastIndex.num
//            firstTradeIndex = lastIndex.id.candles[coinIndex].firstTradeIndex
//        } else {
//            startNum = 0
//            firstTradeIndex = 0
//        }
//        val trades = coinIndexToTrades[coinIndex]
//        return trades
//                .channelIndexed(firstTradeIndex)
//                .takeWhile {
//                    it.value.time < currentTime
//                }
//                .candles(config.startTime, currentTime, config.period)
//                .toItems(startNum)
//    }
//
//    private fun ReceiveChannel<TradesCandle<Long>>.toItems(startNum: Long): ReceiveChannel<CandleItem> = mapIndexed { i, it ->
//        CandleItem(CandleIndex(startNum + i, CandleId(it.firstTradeIndex)), it.candle)
//    }
//
//    private fun List<ReceiveChannel<CandleItem>>.moments(): ReceiveChannel<MomentItem> = zip().map { moment(it) }
//
//    private fun moment(candles: List<CandleItem>): MomentItem {
//        require(candles.isNotEmpty())
//        val index = candles.first().index.num
//        candles.forEach {
//            require(it.index.num == index)
//        }
//        val candleIds = candles.map { it.index.id }
//        val candleValues = candles.map { it.value }
//        val id = MomentId(candleIds)
//        val value = Moment(candleValues)
//        return MomentItem(NumIdIndex(index, id), value)
//    }
//}
//
//fun momentArray(path: Path, config: MomentsConfig) = SyncFileArray(
//        path,
//        MomentsConfig.serializer(),
//        TODO() as KSerializer<MomentIndex>,
//        MomentFixedSerializer(config.coins.size)
//)