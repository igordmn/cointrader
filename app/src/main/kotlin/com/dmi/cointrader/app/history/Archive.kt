package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.history.moment.cachedMoments
import com.dmi.cointrader.app.history.trade.coinToCachedBinanceTrades
import com.dmi.util.io.SyncList
import java.time.Instant

typealias History = List<Moment>

interface Archive {
    suspend fun historyAt(period: Period): History
}

suspend fun archive(exchange: BinanceExchange) {


    //val path = Paths.get("old/data/cache/binance/trades")
    //Files.createDirectories(path)
    //BinanceTradeConfig.serializer(),
    //BinanceTradeState.serializer(),
    //TradeFixedSerializer,
    //return if (marketInfo.isReversed) {
    //    original.map(Trade::reverse)
    //} else {
    //    original
    //}



    //val path = Paths.get("old/data/cache/binance/moments")
    //Files.createDirectories(path.parent)
    //MomentsConfig.serializer(),
    //MomentState.serializer(),
    //MomentFixedSerializer(altCoins.size),
    //MomentsConfig(startTime, period, altCoins),

}

class Archifve(
        private val exchange: BinanceExchange
) {
    suspend fun historyAt(period: Period, size: Int): History {

    }

    suspend fun load(currentTime: Instant) {
        fun coinLog(coin: String) = object: SyncList.Log<Trade> {
            override fun itemsAppended(items: List<Trade>, indices: LongRange) {
                val lastTradeTime = items.last().time
                println("$coin $lastTradeTime")
            }
        }

        val coinToTrades = coinToCachedBinanceTrades(config, constants, api, currentTime, ::coinLog)
        val moments = cachedMoments(config, coinToTrades, currentTime)

        currentTime.sync()
        coinToTrades.mapAsync {
            it.sync()
        }
        moments.sync()
    }
}