package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.moment.Moment
import java.time.Instant

typealias Prices = List<Double>

suspend fun binanceHistory(exchange: BinanceExchange): History {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates
}

class History {
    suspend fun window(period: Period, size: Int): Window {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates
    }

    fun load(currentTime: Instant) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class Window(val prices: Prices, val moments: List<Moment>)
}