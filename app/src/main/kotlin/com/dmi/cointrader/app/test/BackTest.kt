package com.dmi.cointrader.app.test

import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.util.collection.SuspendList
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.MILLIS_PER_HOUR
import com.dmi.util.math.product
import main.test.Config
import java.time.Duration
import kotlin.math.pow

typealias Profits = List<Double>

class BackTest(
        private val network: NeuralNetwork,
        private val moments: SuspendList<Moment>,
        private val config: Config,
        private val testPeriod: Duration
) {
    suspend fun invoke(): Profits {
        TODO()
    }
}

fun Profits.dayly(config: Config) {
    val periodsPerDay = (MILLIS_PER_DAY / config.period.toMillis()).toInt()
    chunked(periodsPerDay).map {
        product(it).pow(periodsPerDay.toDouble() / it.size)
    }
}

fun Profits.hourly(config: Config) {
    val periodsPerHour = (MILLIS_PER_HOUR / config.period.toMillis()).toInt()
    chunked(periodsPerHour).map {
        product(it).pow(periodsPerHour.toDouble() / it.size)
    }
}