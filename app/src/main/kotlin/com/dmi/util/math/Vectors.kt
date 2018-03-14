package com.dmi.util.math

operator fun List<Double>.times(other: List<Double>): List<Double> {
    require(other.size == size)
    return zip(other, Double::times)
}