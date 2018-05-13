package com.dmi.util.math

import org.apache.commons.math3.distribution.GeometricDistribution
import java.lang.Math.pow
import java.util.*

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

fun List<Double>.sortAndRemoveOutliers(percent: Double): List<Double> {
    val removeCount = (size * (percent / 2)).toInt()
    return sorted().drop(removeCount).dropLast(removeCount)
}