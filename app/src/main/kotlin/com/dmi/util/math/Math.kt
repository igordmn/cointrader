package com.dmi.util.math

import java.math.BigDecimal

infix fun Int.floorDiv(y: Int): Int = Math.floorDiv(this, y)
infix fun Int.ceilDiv(y: Int): Int = -Math.floorDiv(-this, y)
infix fun Long.floorDiv(y: Long): Long = Math.floorDiv(this, y)
infix fun Long.ceilDiv(y: Long): Long = -Math.floorDiv(-this, y)

fun Iterable<BigDecimal>.sum(): BigDecimal {
    var sum = BigDecimal.ZERO
    for (value in this) {
        sum += value
    }
    return sum
}