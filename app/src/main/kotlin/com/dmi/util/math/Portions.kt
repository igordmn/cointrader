package com.dmi.util.math

import java.math.BigDecimal
import java.math.RoundingMode

fun List<Double>.portions(): List<Double> {
    val sum = sum()
    return map { it / sum }
}

fun List<BigDecimal>.toDouble(): List<Double> = map { it.toDouble() }