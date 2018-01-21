package exchange.test

import exchange.MarketLimits
import java.math.BigDecimal

class TestMarketLimits(
        private val minAmount: BigDecimal,
        private val amountStep: BigDecimal
) : MarketLimits {
    override fun get() = MarketLimits.Value(minAmount, amountStep)
}