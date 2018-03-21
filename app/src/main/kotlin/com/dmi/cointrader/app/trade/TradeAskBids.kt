package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.archive.IndexedTrade
import kotlinx.coroutines.experimental.channels.ReceiveChannel

data class TradeAskBid<out TRADE_INDEX>(val ask: Double, val bid: Double, val tradeIndex: TRADE_INDEX)

fun <TRADE_INDEX> ReceiveChannel<IndexedTrade<TRADE_INDEX>>.askBids(
        initialAsk: Double,
        initialBid: Double
): ReceiveChannel<TradeAskBid<TRADE_INDEX>> {

}