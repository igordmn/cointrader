package com.dmi.util.math

import java.math.BigDecimal
import java.math.RoundingMode

fun List<BigDecimal>.portions(): List<Double> {
    val sum = sum().toDouble()
    return map { it.toDouble() / sum }
}

fun List<BigDecimal>.toDouble(): List<Double> = map { it.toDouble() }