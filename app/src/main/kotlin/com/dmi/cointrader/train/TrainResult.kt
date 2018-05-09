package com.dmi.cointrader.train

import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.cointrader.trade.*
import com.dmi.util.math.geoMean
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

fun parseResults(file: Path): List<SavedTrainResult> {
    return file.toFile()
            .readLines()
            .filter {
                it.isNotEmpty() && it[0].isDigit()
            }
            .map {
                val values = it.replace("  ", " ").replace("  ", " ").replace("  ", " ").split(" ")
                SavedTrainResult(
                        it,
                        step = values[0].toInt(),
                        test0DayProfitMedian = values[4].toDouble()
                )
            }
}

data class SavedTrainResult(val str: String, val step: Int, val test0DayProfitMedian: Double)