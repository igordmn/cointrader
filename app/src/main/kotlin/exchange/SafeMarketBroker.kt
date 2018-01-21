package exchange

import java.math.BigDecimal
import java.math.RoundingMode

class SafeMarketBroker(
        private val original: MarketBroker,
        private val limits: MarketLimits
) : MarketBroker {
    suspend override fun buy(amount: BigDecimal) {
        processAmount(amount) { newAmount ->
            original.buy(newAmount)
        }
    }

    suspend override fun sell(amount: BigDecimal) {
        processAmount(amount) { newAmount ->
            original.sell(newAmount)
        }
    }

    private suspend fun processAmount(amount: BigDecimal, action: suspend (newAmount: BigDecimal) -> Unit) {
        require(amount >= BigDecimal.ZERO)

        val limits = limits.get()
        val roundedAmount = if (limits.amountStep > BigDecimal.ZERO) {
            amount.divide(limits.amountStep, 0, RoundingMode.FLOOR) * limits.amountStep
        } else {
            amount
        }

        if (roundedAmount >= limits.minAmount) {
            action(roundedAmount)
        }
    }
}