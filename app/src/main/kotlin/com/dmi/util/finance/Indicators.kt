package com.dmi.util.finance

import kotlin.math.pow
import kotlin.math.ln
import kotlin.math.sqrt

fun maximumDrawdawn(profits: List<Double>): Double {
    var maxDrawdown = 0.0
    var maxCapital = 0.0
    var capital = 1.0
    for (profit in profits) {
        capital *= profit
        if (capital >= maxCapital) {
            maxCapital = capital
        } else {
            val drawdown = 1.0 - capital / maxCapital
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown
            }
        }
    }
    return maxDrawdown
}

fun sortinoRatio(profits: List<Double>): Double {
    fun g(x: Double) = ln(x).coerceAtMost(0.0).pow(2)
    return sqrt(profits.map(::g).average())
}