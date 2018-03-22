package com.dmi.cointrader.app.archive

import kotlinx.serialization.Serializable

@Serializable
data class AskBidTrade(val ask: Double, val bid: Double)

fun Trade.toAskBid(previous: AskBidTrade?): AskBidTrade = if (previous == null) {
    AskBidTrade(
            ask = price,
            bid = price
    )
} else {
    val isAsk = previous.ask - price <= price - previous.bid
    AskBidTrade(
            ask = if (isAsk) price else previous.ask,
            bid = if (!isAsk) price else previous.bid
    )
}