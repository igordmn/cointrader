package com.dmi.util.math

import org.apache.commons.math3.distribution.GeometricDistribution
import java.lang.Math.pow
import java.util.*
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln

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

fun List<Double>.normalGeoMean(): Double {
    var s = 0.0
    var ws = 0.0
    sorted().forEachIndexed { index, it ->
        val x = 2 * index.toDouble() / (size - 1) - 1  // From -1.0 to 1.0
        val w = exp(-(x * x) * PI)  // Normal distribution
        s += w * ln(it)
        ws += w
    }
    return exp(s / ws)
}

fun List<Double>.limitOutliers(percent: Double): List<Double> {
    val removeCount = (size * (percent / 2)).toInt()
    val sorted = sorted().drop(removeCount).dropLast(removeCount)
    val min = sorted.first()
    val max = sorted.last()
    return map { it.coerceIn(min..max) }
}