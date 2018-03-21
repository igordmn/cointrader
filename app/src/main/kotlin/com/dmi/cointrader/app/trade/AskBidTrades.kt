package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.archive.IndexedTrade
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce

data class AskBidTrade<out TRADE_INDEX>(val ask: Double, val bid: Double, val trade: IndexedTrade<TRADE_INDEX>)

fun <TRADE_INDEX> ReceiveChannel<IndexedTrade<TRADE_INDEX>>.askBids(
        last: AskBidTrade<TRADE_INDEX>?
): ReceiveChannel<AskBidTrade<TRADE_INDEX>> = produce {
    var askBid = last
    consumeEach { trade ->
        fun initial() = AskBidTrade(ask = trade.value.price, bid = trade.value.price, trade = trade)

        fun AskBidTrade<TRADE_INDEX>.next(): AskBidTrade<TRADE_INDEX> {
            val isAsk = ask - trade.value.price <= trade.value.price - bid
            return AskBidTrade(
                    ask = if (isAsk) trade.value.price else ask,
                    bid = if (!isAsk) trade.value.price else bid,
                    trade = trade
            )
        }

        val newAskBid = askBid?.next() ?: initial()
        send(newAskBid)
        askBid = newAskBid
    }
}