package exchange

import java.math.BigDecimal

interface MarketLimits {
    fun get(): Value

    data class Value(
            val amountStep: BigDecimal,
            val minTotalPrice: BigDecimal
    )
}