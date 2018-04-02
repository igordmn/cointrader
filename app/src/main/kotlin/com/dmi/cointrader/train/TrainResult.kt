package com.dmi.cointrader.train

import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.cointrader.trade.*
import com.dmi.util.lang.times
import com.dmi.util.math.downsideDeviation
import com.dmi.util.math.geoMean
import com.dmi.util.math.maximumDrawdawn
import kotlin.math.pow

fun trainResult(
        space: PeriodSpace,
        tradePeriods: Int,
        step: Int,
        movingAverageCount: Int,
        previousResults: List<TrainResult>,
        trainProfits: Profits,
        testCapitals: Capitals,
        validationCapitals: Capitals
): TrainResult {
    val tradePeriodsPerDay = space.periodsPerDay() / tradePeriods.toDouble()
    val tradeDuration = space.duration * tradePeriods

    fun trainTestResult(capitals: Profits, previousResults: List<TrainResult.Test>): TrainResult.Test {
        val profits = capitals.profits()
        val dayProfit = profits.daily(tradeDuration).let(::geoMean)
        val averageDayProfit = if (previousResults.size >= movingAverageCount) {
            geoMean(previousResults.slice(previousResults.size - movingAverageCount until previousResults.size).map { it.dayProfit })
        } else {
            null
        }
        val hourlyProfits = profits.hourly(tradeDuration)
        val downsideDeviation: Double = hourlyProfits.let(::downsideDeviation)
        val maximumDrawdawn: Double = hourlyProfits.let(::maximumDrawdawn)
        return TrainResult.Test(capitals, dayProfit, averageDayProfit, downsideDeviation, maximumDrawdawn)
    }

    return TrainResult(
            step,
            geoMean(trainProfits).pow(tradePeriodsPerDay),
            trainTestResult(testCapitals, previousResults.map { it.test }),
            trainTestResult(validationCapitals, previousResults.map { it.validation })
    )
}

data class TrainResult(val step: Int, val trainDayProfit: Double, val test: Test, val validation: Test) {
    data class Test(val capitals: Capitals, val dayProfit: Double, val averageDayProfit: Double?, val hourlyNegativeDeviation: Double, val hourlyMaximumDrawdawn: Double) {
        override fun toString(): String {
            val dayProfit = "%.3f".format(dayProfit)
            val averageDayProfit = "%.3f".format(averageDayProfit)
            val hourlyNegativeDeviation = "%.5f".format(hourlyNegativeDeviation)
            val hourlyMaximumDrawdawn = "%.2f".format(hourlyMaximumDrawdawn)
            return "$dayProfit $averageDayProfit $hourlyNegativeDeviation $hourlyMaximumDrawdawn"
        }
    }

    override fun toString(): String {
        val trainDayProfit = "%.3f".format(trainDayProfit)
        return "$step   $trainDayProfit   $test   $validation"
    }
}