package com.dmi.util.math

import org.apache.commons.math3.distribution.GeometricDistribution
import java.lang.Math.pow
import java.util.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

fun Random.nextInt(range: IntRange) = range.first + nextInt(range.last - 1 - range.first)

fun GeometricDistribution.limitSample(max: Int): Int {
    var ran = sample()
    while (ran > max) {
        ran = sample()
    }
    return ran
}

fun List<Double>.product(): Double = reduce { acc, value -> acc * value }

fun List<Double>.geoMean(): Double = pow(product(), 1.0 / size)

fun List<Double>.limitOutliers(percent: Double): List<Double> {
    val removeCount = (size * (percent / 2)).toInt()
    val sorted = sorted().drop(removeCount).dropLast(removeCount)
    val min = sorted.first()
    val max = sorted.last()
    return map { it.coerceIn(min..max) }
}

fun List<Double>.sharpeRatio() = average() / (1e-10 + stddev())

fun List<Double>.stddev() : Double {
    val mean = average()
    fun f(x: Double) = (x - mean) * (x - mean)
    return sqrt(map(::f).average())
}