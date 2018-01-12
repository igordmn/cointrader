package util.math

import java.math.BigDecimal

class Portions(val percents: List<BigDecimal>) {
    init {
        require(percents.sum() == BigDecimal.ONE)
    }
}

fun List<BigDecimal>.portions(scale: Int): Portions {
    val sum = sum()
    return Portions(map { it.divide(sum, scale) })
}