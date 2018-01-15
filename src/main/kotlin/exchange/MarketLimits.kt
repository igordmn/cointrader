package exchange

import java.math.BigDecimal

interface MarketLimits {
    fun get(): Value

    data class Value(
            val minAmount: BigDecimal,
            val maxAmount: BigDecimal,
            val amountStep: BigDecimal,
            val minTotalPrice: BigDecimal
    )
}