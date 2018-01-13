package exchange.test

import exchange.Market
import exchange.MarketHistory
import util.math.min
import java.math.BigDecimal

@Suppress("RedundantSuspendModifier")
class TestMarketWithRealPrices(
        private val fromCoin: String,
        private val toCoin: String,
        private val portfolio: TestPortfolio,
        private val prices: Prices
) : Market {
    override fun history(): MarketHistory = prices.history

    override suspend fun buy(amount: BigDecimal) {
        val currentPrice = prices.current()
        portfolio.transaction {
            val fromAmount = it[fromCoin]
            val fromSellAmount = amount * currentPrice
            if (fromSellAmount <= fromAmount) {
                it[fromCoin] = it[fromCoin] - fromSellAmount
                it[toCoin] = it[toCoin] + amount
            } else {
                it[fromCoin] = BigDecimal.ZERO
                it[toCoin] = it[toCoin] + amount
            }
        }
    }

    override suspend fun sell(amount: BigDecimal) {
        val currentPrice = prices.current()
        portfolio.transaction {
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

    interface Prices {
        val history: MarketHistory
        suspend fun current(): BigDecimal
    }
}