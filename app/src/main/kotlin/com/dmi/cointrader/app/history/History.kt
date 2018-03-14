package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.Periods

typealias Prices = List<Double>

class History {
    suspend fun window(period: Period, size: Int): Window {

    }

    class Window(val prices: Prices)
}

suspend fun binanceHistory(exchange: BinanceExchange): History {

}