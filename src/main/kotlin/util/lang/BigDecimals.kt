package util.lang

import java.math.BigDecimal
import java.math.RoundingMode

fun Map<String, BigDecimal>.roundValues(scale: Int): Map<String, BigDecimal> = mapValues { it.value.round(scale) }
fun BigDecimal.round(scale: Int): BigDecimal = setScale(scale, RoundingMode.HALF_UP)