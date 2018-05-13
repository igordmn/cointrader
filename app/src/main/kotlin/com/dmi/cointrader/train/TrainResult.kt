package com.dmi.cointrader.train

import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.cointrader.trade.*
import com.dmi.util.lang.indexOfMax
import com.dmi.util.math.geoMean
import kotlinx.serialization.Serializable
import java.nio.file.Path
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
            trainProfits.geoMean().pow(tradePeriodsPerDay),
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

@Serializable
data class TrainResult(val step: Int, val trainDayProfit: Double, val tests: List<TradeSummary>) {
    override fun toString(): String {
        val trainDayProfit = "%.3f".format(trainDayProfit)
        val tests = tests.joinToString("   ")
        return "$step   $trainDayProfit   $tests"
    }
}