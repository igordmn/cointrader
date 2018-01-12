package util.math

import java.math.BigDecimal

fun Iterable<BigDecimal>.sum(): BigDecimal {
    var sum = BigDecimal.ZERO
    for (value in this) {
        sum += value
    }
    return sum
}