package com.dmi.cointrader.train

import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.cointrader.trade.*
import com.dmi.util.lang.times
import com.dmi.util.math.downsideDeviation
import com.dmi.util.math.geoMean
import com.dmi.util.math.maximumDrawdawn
import kotlin.math.pow

fun trainResult(space: PeriodSpace, tradePeriods: Int, step: Int, trainProfits: Profits, testProfits: Profits, validationProfits: Profits): TrainResult {
    val tradePeriodsPerDay = space.periodsPerDay() / tradePeriods.toDouble()
    val tradeDuration = space.duration * tradePeriods

    fun trainTestResult(tradeProfits: Profits): TrainResult.Test {
        val dayProfit = tradeProfits.daily(tradeDuration).let(::geoMean)
        val hourlyProfits = tradeProfits.hourly(tradeDuration)
        val downsideDeviation: Double = hourlyProfits.let(::downsideDeviation)
        val maximumDrawdawn: Double = hourlyProfits.let(::maximumDrawdawn)
        return TrainResult.Test(dayProfit, downsideDeviation, maximumDrawdawn)
    }

    return TrainResult(
            step,
            geoMean(trainProfits).pow(tradePeriodsPerDay),
            trainTestResult(testProfits),
            trainTestResult(validationProfits)
    )
}

data class TrainResult(val step: Int, val trainDayProfit: Double, val test: Test, val validation: Test) {
    data class Test(val dayProfit: Double, val hourlyNegativeDeviation: Double, val hourlyMaximumDrawdawn: Double) {
        override fun toString(): String {
            val dayProfit = "%.3f".format(dayProfit)
            val hourlyNegativeDeviation = "%.5f".format(hourlyNegativeDeviation)
            val hourlyMaximumDrawdawn = "%.2f".format(hourlyMaximumDrawdawn)
            return "$dayProfit $hourlyNegativeDeviation $hourlyMaximumDrawdawn"
        }
    }

    override fun toString(): String {
        val trainDayProfit = "%.3f".format(trainDayProfit)
        return "$step   $trainDayProfit   $test   $validation"
    }
}