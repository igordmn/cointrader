package com.dmi.cointrader.binance

import java.time.Clock
import com.dmi.util.lang.minus

suspend fun binanceClock(exchange: BinanceExchange): Clock {
    val systemClock = Clock.systemUTC()
    val serverTime = exchange.currentTime()
    val diff = serverTime - systemClock.instant()
    return Clock.offset(systemClock, diff)
}