package util.math

import java.math.BigDecimal
import java.math.RoundingMode

fun Iterable<BigDecimal>.sum(): BigDecimal {
    var sum = BigDecimal.ZERO
    for (value in this) {
        sum += value
    }
    return sum
}

fun min(v1: BigDecimal, v2: BigDecimal): BigDecimal = if (v1 < v2) v1 else v2
fun min(vararg values: BigDecimal): BigDecimal = values.min()!!
fun max(v1: BigDecimal, v2: BigDecimal): BigDecimal = if (v1 > v2) v1 else v2
fun max(vararg values: BigDecimal): BigDecimal = values.max()!!

fun Map<String, BigDecimal>.roundValues(scale: Int): Map<String, BigDecimal> = mapValues { it.value.round(scale) }
fun BigDecimal.round(scale: Int): BigDecimal = setScale(scale, RoundingMode.HALF_UP)

infix fun BigDecimal.equalsWithoutScale(other: BigDecimal) = compareTo(other) == 0
infix fun BigDecimal.notEqualsWithoutScale(other: BigDecimal) = compareTo(other) != 0