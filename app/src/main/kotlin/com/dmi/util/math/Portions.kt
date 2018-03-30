package com.dmi.util.math

import java.math.BigDecimal
import java.math.RoundingMode

fun List<BigDecimal>.portions(): List<Double> {
    val doubles = map { it.toDouble() }
    val sum = doubles.sum()
    return doubles.map { it / sum }
}

fun List<BigDecimal>.toDouble(): List<Double> = map { it.toDouble() }