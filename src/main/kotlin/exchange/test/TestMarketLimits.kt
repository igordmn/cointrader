package exchange.test

import exchange.MarketLimits
import java.math.BigDecimal

class TestMarketLimits(
        private val amountStep: BigDecimal,
        private val minTotalPrice: BigDecimal
) : MarketLimits {
    override fun get() = MarketLimits.Value(amountStep, minTotalPrice)
}