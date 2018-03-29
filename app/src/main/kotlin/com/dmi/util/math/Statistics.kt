package com.dmi.util.math

import org.apache.commons.math3.distribution.GeometricDistribution
import java.util.*
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

fun Random.nextInt(range: IntRange) = range.first + nextInt(range.last - 1 - range.first)
fun Random.nextLong(range: LongRange) = range.first + nextLong(range.last - 1 - range.first)
fun Random.nextLong(max : Long) = nextLong() % max

fun GeometricDistribution.sampleIn(range: IntRange): Int {
    var ran = sample()
    while(ran > range.endInclusive + 1 - range.start) {
        ran = sample()
    }
    return range.endInclusive + 1 - ran
}

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

fun downsideDeviation(values: List<Double>): Double {
    fun g(x: Double) = ln(x).coerceAtMost(0.0).pow(2)
    return sqrt(values.map(::g).average())
}

fun product(values: List<Double>): Double = values.reduce { acc, value -> acc * value }
fun geoMean(values: List<Double>): Double = Math.pow(product(values), 1.0 / values.size)