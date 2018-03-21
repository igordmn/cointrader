package com.dmi.cointrader.app.archive

data class AskBidTrade(val ask: Double, val bid: Double)

fun Trade.toAskBid(previous: AskBidTrade): AskBidTrade {
    val isAsk = previous.ask - price <= price - previous.bid
    return AskBidTrade(
            ask = if (isAsk) price else previous.ask,
            bid = if (!isAsk) price else previous.bid
    )
}