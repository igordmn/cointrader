package com.dmi.util.math

import org.apache.commons.math3.distribution.GeometricDistribution

fun GeometricDistribution.rangeSample(range: IntRange): Int {
    var ran = sample()
    while(ran > range.endInclusive + 1 - range.start) {
        ran = sample()
    }
    return range.endInclusive + 1 - ran
}