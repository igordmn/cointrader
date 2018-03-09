package com.dmi.util.math

import java.math.BigDecimal
import java.math.RoundingMode

fun <K> Map<K, BigDecimal>.portions(scale: Int): Map<K, BigDecimal> {
    val sum = values.sum()
    return mapValues { it.value.divide(sum, scale, RoundingMode.HALF_UP) }
}

fun List<Double>.portions(): List<Double> {
    val sum = sum()
    return map{ it / sum }
}