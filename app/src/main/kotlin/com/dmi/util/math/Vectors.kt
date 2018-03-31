package com.dmi.util.math

import java.math.BigDecimal

@JvmName("times1")
operator fun List<Double>.times(other: List<Double>): List<Double> {
    require(other.size == size)
    return zip(other, Double::times)
}

operator fun List<Double>.times(other: Double): List<Double> {
    return map { it * other }
}

@JvmName("div1")
operator fun List<Double>.div(other: List<Double>): List<Double> {
    require(other.size == size)
    return zip(other, Double::div)
}

@JvmName("times2")
operator fun List<BigDecimal>.times(other: List<BigDecimal>): List<BigDecimal> {
    require(other.size == size)
    return zip(other, BigDecimal::times)
}