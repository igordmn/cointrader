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
        portfolio.modify {
            val limits = limits.get()
            val roundedAmount = amount.divide(limits.amountStep, 0, RoundingMode.FLOOR) * limits.amountStep
            if (roundedAmount >= limits.minTotalPrice) {
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
    }

    override suspend fun sell(amount: BigDecimal) {
        val currentPrice = price.current()
        portfolio.modify {
            val limits = limits.get()
            val roundedAmount = amount.divide(limits.amountStep, 0, RoundingMode.FLOOR) * limits.amountStep
            if (roundedAmount >= limits.minTotalPrice) {
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
}