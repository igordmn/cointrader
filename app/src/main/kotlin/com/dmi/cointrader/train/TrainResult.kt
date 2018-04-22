package com.dmi.cointrader.train

import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.cointrader.info.ChartData
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
        previousResults: List<TrainResult>,
        trainProfits: Profits,
        testCapitals: List<Capitals>
): TrainResult {
    val tradePeriodsPerDay = space.periodsPerDay() / tradePeriods.toDouble()
    return TrainResult(
            step,
            geoMean(trainProfits).pow(tradePeriodsPerDay),
            testCapitals.mapIndexed { i, it ->
                tradeSummary(
                        space,
                        tradePeriods,
                        it,
                        previousResults.map { it.tests[i] }
                )
            }
    )
}

data class TrainResult(val step: Int, val trainDayProfit: Double, val tests: List<TradeSummary>) {
    override fun toString(): String {
        val trainDayProfit = "%.3f".format(trainDayProfit)
        val tests = tests.joinToString("   ")
        return "$step   $trainDayProfit   $tests"
    }
}