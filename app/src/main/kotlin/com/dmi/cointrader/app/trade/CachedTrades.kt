package com.dmi.cointrader.app.trade

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import java.time.Instant

class CachedTrades {
    fun loadBefore(market: String, instant: Instant) {
        TODO()
    }

    fun trades(market: String): ReceiveChannel<Trade> {
        TODO()
    }
}