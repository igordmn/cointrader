package util.math

import java.math.BigDecimal

fun Iterable<BigDecimal>.sum(): BigDecimal {
    var sum = BigDecimal.ZERO
    for (value in this) {
        sum += value
    }
    return sum
}

fun min(v1: BigDecimal, v2: BigDecimal): BigDecimal = if (v1 > v2) v2 else v1