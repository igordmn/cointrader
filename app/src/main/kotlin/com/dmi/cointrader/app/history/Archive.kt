package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.moment.cachedMoments
import com.dmi.cointrader.app.trade.Trade
import com.dmi.cointrader.app.trade.coinToCachedBinanceTrades
import com.dmi.util.concurrent.mapAsync
import com.dmi.util.io.SyncList
import java.time.Instant

typealias History = List<Moment>

class Archive(
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