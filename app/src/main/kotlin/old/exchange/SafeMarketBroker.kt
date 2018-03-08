package old.exchange

import org.slf4j.Logger
import java.math.BigDecimal
import java.math.RoundingMode

class SafeMarketBroker(
        private val original: MarketBroker,
        private val limits: MarketLimits,
        private val attemptCount: Int,
        private val attemptAmountDecay: BigDecimal,
        private val log: Logger
) : MarketBroker {
    suspend override fun buy(amount: BigDecimal) {
        processAmount(amount, "buy") { newAmount ->
            original.buy(newAmount)
        }
    }

    suspend override fun sell(amount: BigDecimal) {
        processAmount(amount, "sell") { newAmount ->
            original.sell(newAmount)
        }
    }

    private suspend fun processAmount(amount: BigDecimal, logMethod: String, action: suspend (newAmount: BigDecimal) -> Unit) {
        var attempt = 0
        var currentAmount = amount
        while (true) {
            try {
                limitAmount(currentAmount, action)
            } catch (e: MarketBroker.Error.InsufficientBalance) {
                log.info("InsufficientBalance ($logMethod). Attempt $attempt   amount $currentAmount")
                if (attempt == attemptCount - 1) {
                    throw e
                } else {
                    attempt++
                    currentAmount *= attemptAmountDecay
                    continue
                }
            }
            break
        }
    }

    private suspend fun limitAmount(amount: BigDecimal, action: suspend (newAmount: BigDecimal) -> Unit) {
        if (amount < BigDecimal.ZERO) {
            throw MarketBroker.Error.WrongAmount()
        }

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