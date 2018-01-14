package exchange.test

import exchange.MarketOrders
import exchange.MarketPrice
import java.math.BigDecimal

class TestMarketOrders(
        private val fromCoin: String,
        private val toCoin: String,
        private val portfolio: TestPortfolio,
        private val price: MarketPrice
) : MarketOrders {
    override suspend fun buy(amount: BigDecimal) {
        val currentPrice = price.current()
        portfolio.modify {
            val fromAmount = it[fromCoin]
            val fromSellAmount = amount * currentPrice
            if (fromSellAmount <= fromAmount) {
                it[fromCoin] = it[fromCoin] - fromSellAmount
                it[toCoin] = it[toCoin] + amount
            } else {
                it[fromCoin] = BigDecimal.ZERO
                it[toCoin] = it[toCoin] + fromAmount / currentPrice
            }
        }
    }

    override suspend fun sell(amount: BigDecimal) {
        val currentPrice = price.current()
        portfolio.modify {
            val toAmount = it[toCoin]
            if (amount <= toAmount) {
                it[fromCoin] = it[fromCoin] + amount * currentPrice
                it[toCoin] = it[toCoin] - amount
            } else {
                it[fromCoin] = it[fromCoin] + toAmount * currentPrice
                it[toCoin] = BigDecimal.ZERO
            }
        }
    }
}