package com.dmi.cointrader.app.binance

import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.util.atom.ReadAtom
import java.time.Instant

class BinanceTime(
        private val api: BinanceAPI
) : ReadAtom<Instant> {
    suspend override fun invoke(): Instant {
        val serverTime = api.serverTime()
        return Instant.ofEpochMilli(serverTime.serverTime)
    }
}