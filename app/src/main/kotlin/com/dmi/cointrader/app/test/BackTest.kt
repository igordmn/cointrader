package com.dmi.cointrader.app.test

data class BackTestResult(
        val hourCapitals: List<Double>,
        val dayProfit: Double,
        val hourNegativeDeviation: Double,
        val hourMaximumDrawdawn: Double
)

fun backTest(): BackTestResult {

}