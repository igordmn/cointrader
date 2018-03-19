package com.dmi.cointrader.app.test

import com.dmi.cointrader.app.performtrade.TradeConfig
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.MILLIS_PER_HOUR
import com.dmi.util.math.product
import java.time.Duration
import kotlin.math.pow

typealias Profits = List<Double>

fun Profits.dayly(period: Duration): Profits {
    val periodsPerDay = (MILLIS_PER_DAY / period.toMillis()).toInt()
    return chunked(periodsPerDay).map {
        product(it).pow(periodsPerDay.toDouble() / it.size)
    }
}

fun Profits.hourly(period: Duration): Profits {
    val periodsPerHour = (MILLIS_PER_HOUR / period.toMillis()).toInt()
    return chunked(periodsPerHour).map {
        product(it).pow(periodsPerHour.toDouble() / it.size)
    }
}