package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.Periods
import java.time.Instant

typealias Prices = List<Double>

class History {
    suspend fun window(period: Period, size: Int): Window {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates
    }

    fun loadBefore(time: Instant) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class Window(val prices: Prices)
}

suspend fun binanceHistory(exchange: BinanceExchange): History {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates
}