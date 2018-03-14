package com.dmi.cointrader.app.binance

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

suspend fun binanceClock(exchange: BinanceExchange): Clock {
    val systemClock = Clock.systemUTC()
    val serverTime = exchange.currentTime()
    val diff = Duration.between(systemClock.instant(), serverTime)
    return Clock.offset(systemClock, diff)
}