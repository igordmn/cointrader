package util.math

import java.math.BigDecimal
import java.math.RoundingMode

fun <K> Map<K, BigDecimal>.portions(scale: Int): Map<K, BigDecimal> {
    val sum = values.sum()
    return mapValues { it.value.divide(sum, scale, RoundingMode.HALF_UP) }
}