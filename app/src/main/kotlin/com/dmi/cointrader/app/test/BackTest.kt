package com.dmi.cointrader.app.test

import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.util.collection.SuspendList
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.MILLIS_PER_HOUR
import com.dmi.util.math.product
import com.dmi.cointrader.main.Config
import kotlin.math.pow

typealias Profits = List<Double>

fun Profits.dayly(config: Config): Profits {
    val periodsPerDay = (MILLIS_PER_DAY / config.period.toMillis()).toInt()
    return chunked(periodsPerDay).map {
        product(it).pow(periodsPerDay.toDouble() / it.size)
    }
}

fun Profits.hourly(config: Config): Profits {
    val periodsPerHour = (MILLIS_PER_HOUR / config.period.toMillis()).toInt()
    return chunked(periodsPerHour).map {
        product(it).pow(periodsPerHour.toDouble() / it.size)
    }
}

suspend fun backTest(days: Double) {

}