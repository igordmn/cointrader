package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.archive.IndexedTrade
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce

data class TradeAskBid<out TRADE_INDEX>(val ask: Double, val bid: Double, val tradeIndex: TRADE_INDEX)

fun <TRADE_INDEX> ReceiveChannel<IndexedTrade<TRADE_INDEX>>.askBids(
        last: TradeAskBid<TRADE_INDEX>?
): ReceiveChannel<TradeAskBid<TRADE_INDEX>> = produce {
    var askBid = last
    consumeEach { trade ->
        fun initial() = TradeAskBid(ask = trade.value.price, bid = trade.value.price, tradeIndex = trade.index)

        fun TradeAskBid<TRADE_INDEX>.next(): TradeAskBid<TRADE_INDEX> {
            val isAsk = ask - trade.value.price <= trade.value.price - bid
            return TradeAskBid(
                    ask = if (isAsk) trade.value.price else ask,
                    bid = if (!isAsk) trade.value.price else bid,
                    tradeIndex = trade.index
            )
        }

        val newAskBid = askBid?.next() ?: initial()
        send(newAskBid)
        askBid = newAskBid
    }
}