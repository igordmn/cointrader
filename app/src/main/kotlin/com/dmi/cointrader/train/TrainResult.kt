package com.dmi.cointrader.train

import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.cointrader.trade.*
import com.dmi.util.math.downsideDeviation
import com.dmi.util.math.geoMean
import com.dmi.util.math.maximumDrawdawn
import kotlin.math.pow

fun trainResult(space: PeriodSpace, step: Int, trainProfits: Profits, testResults: List<TradeResult>, validationResults: List<TradeResult>): TrainResult {
    val periodsPerDay = space.periodsPerDay()
    val period = space.duration

    fun trainTestResult(tradeResults: List<TradeResult>): TrainResult.Test {
        val profits = tradeResults.capitals().profits()
        val dayProfit = profits.daily(period).let(::geoMean)
        val hourlyProfits = profits.hourly(period)
        val downsideDeviation: Double = hourlyProfits.let(::downsideDeviation)
        val maximumDrawdawn: Double = hourlyProfits.let(::maximumDrawdawn)
        return TrainResult.Test(dayProfit, downsideDeviation, maximumDrawdawn)
    }

    return TrainResult(
            step,
            geoMean(trainProfits).pow(periodsPerDay),
            trainTestResult(testResults),
            trainTestResult(validationResults)
    )
}

data class TrainResult(val step: Int, val trainDayProfit: Double, val test: Test, val validation: Test) {
    data class Test(val dayProfit: Double, val hourlyNegativeDeviation: Double, val hourlyMaximumDrawdawn: Double)
}