package com.dmi.cointrader.app.trade

import com.dmi.cointrader.data.Market
import com.dmi.cointrader.data.TradeChunk
import io.objectbox.Box

class TradesCache(private val box: Box<TradeChunk>) {
    suspend fun insert(trade: BinanceTrade) {

    }
//
//    suspend fun lastAggregatedId(): Long {
//
//    }

//    fun trades(market: Market)
}