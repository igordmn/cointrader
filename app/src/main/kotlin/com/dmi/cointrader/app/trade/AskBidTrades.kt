package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.archive.Trade
import com.dmi.util.restorable.RestorableSource
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.serialization.Serializable

@Serializable
data class AskBid

data class AskBidTrade(val ask: Double, val bid: Double)

fun <TRADE_SOURCE: RestorableSource<*, Trade>> TRADE_SOURCE.askBids() = object: RestorableSource<>




        fun gg() {
            ReceiveChannel<AskBidTrade<TRADE_INDEX>> = produce {
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