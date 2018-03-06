package com.dmi.cointrader.app.test

class BackTest {
    suspend fun invoke(): Result {
        TODO()
    }

    data class Result(
            val hourCapitals: List<Double>,
            val dayProfit: Double,
            val hourNegativeDeviation: Double,
            val hourMaximumDrawdawn: Double
    )
}
