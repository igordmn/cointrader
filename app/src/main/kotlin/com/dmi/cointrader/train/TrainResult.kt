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
        testCapitals: List<Capitals>
): TrainResult {
    val tradePeriodsPerDay = space.periodsPerDay() / tradePeriods.toDouble()
    val tradeDuration = space.duration * tradePeriods

    fun trainTestResult(capitals: Capitals, previousResults: List<TrainResult.Test>): TrainResult.Test {
        val profits = capitals.profits()
        val dayProfit = profits.daily(tradeDuration).let(::geoMean)
        val averageDayProfit = if (previousResults.size >= movingAverageCount) {
            geoMean(previousResults.slice(previousResults.size - movingAverageCount until previousResults.size).map { it.dayProfit })
        } else {
            null
        }
        val downsideDeviation: Double = profits.let(::downsideDeviation)
        val maximumDrawdawn: Double = profits.let(::maximumDrawdawn)
        return TrainResult.Test(dayProfit, averageDayProfit, downsideDeviation, maximumDrawdawn)
    }

    val dayProfitsChart = run {
        val profitDays = testCapitals[0].indices.map { it / tradePeriodsPerDay }
        TrainResult.ChartData(profitDays.toDoubleArray(), testCapitals[0].toDoubleArray())
    }

    return TrainResult(
            step,
            geoMean(trainProfits).pow(tradePeriodsPerDay),
            testCapitals.mapIndexed { i, it -> trainTestResult(it, previousResults.map { it.tests[i] })},
            dayProfitsChart
    )
}

data class TrainResult(val step: Int, val trainDayProfit: Double, val tests: List<Test>, val firstTestChart: ChartData) {
    data class Test(val dayProfit: Double, val averageDayProfit: Double?, val negativeDeviation: Double, val maximumDrawdawn: Double) {
        override fun toString(): String {
            val dayProfit = "%.3f".format(dayProfit)
            val averageDayProfit = if (averageDayProfit != null) "%.3f".format(averageDayProfit) else "-----"
            val negativeDeviation = "%.5f".format(negativeDeviation)
            val maximumDrawdawn = "%.2f".format(maximumDrawdawn)
            return "$dayProfit $averageDayProfit $negativeDeviation $maximumDrawdawn"
        }
    }

    class ChartData(val x: DoubleArray, val y: DoubleArray)

    override fun toString(): String {
        val trainDayProfit = "%.3f".format(trainDayProfit)
        val tests = tests.joinToString("   ")
        return "$step   $trainDayProfit   $tests"
    }
}