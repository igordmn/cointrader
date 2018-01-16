package exchange.test

import exchange.MarketBroker
import exchange.MarketLimits
import exchange.MarketPrice
import java.math.BigDecimal
import java.math.RoundingMode

class TestMarketBroker(
        private val fromCoin: String,
        private val toCoin: String,
        private val portfolio: TestPortfolio,
        private val price: MarketPrice,
        private val fee: BigDecimal,
        private val limits: MarketLimits
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) {
        val currentPrice = price.current()
        val limits = limits.get()
        val roundedAmount = if (limits.amountStep > BigDecimal.ZERO) {
            amount.divide(limits.amountStep, 0, RoundingMode.FLOOR) * limits.amountStep
        } else {
            amount
        }
        if (roundedAmount * currentPrice >= limits.minTotalPrice) {
            buyToPortfolio(roundedAmount, currentPrice)
        }
    }

    private suspend fun buyToPortfolio(amount: BigDecimal, currentPrice: BigDecimal) {
        portfolio.modify {
            val fromAmount = it[fromCoin]
            val fromSellAmount = amount * currentPrice
            if (fromSellAmount <= fromAmount) {
                it[fromCoin] = it[fromCoin] - fromSellAmount
                it[toCoin] = it[toCoin] + amount * (BigDecimal.ONE - fee)
            } else {
                it[fromCoin] = BigDecimal.ZERO
                it[toCoin] = it[toCoin] + fromAmount / currentPrice * (BigDecimal.ONE - fee)
            }
        }
    }

    override suspend fun sell(amount: BigDecimal) {
        val currentPrice = price.current()
        val limits = limits.get()
        val roundedAmount = if (limits.amountStep > BigDecimal.ZERO) {
            amount.divide(limits.amountStep, 0, RoundingMode.FLOOR) * limits.amountStep
        } else {
            amount
        }
        if (roundedAmount * currentPrice >= limits.minTotalPrice) {
            sellFromPortfolio(roundedAmount, currentPrice)
        }
    }

    private suspend fun sellFromPortfolio(amount: BigDecimal, currentPrice: BigDecimal) {
        portfolio.modify {
            val toAmount = it[toCoin]
            if (amount <= toAmount) {
                it[fromCoin] = it[fromCoin] + amount * currentPrice * (BigDecimal.ONE - fee)
                it[toCoin] = it[toCoin] - amount
            } else {
                it[fromCoin] = it[fromCoin] + toAmount * currentPrice * (BigDecimal.ONE - fee)
                it[toCoin] = BigDecimal.ZERO
            }
        }
    }
}