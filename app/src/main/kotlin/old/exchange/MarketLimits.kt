package old.exchange

import java.math.BigDecimal

interface MarketLimits {
    fun get(): Value

    data class Value(
            val minAmount: BigDecimal,
            val amountStep: BigDecimal
    ) {
        init {
            require(minAmount >= BigDecimal.ZERO)
            require(amountStep >= BigDecimal.ZERO)
        }
    }
}