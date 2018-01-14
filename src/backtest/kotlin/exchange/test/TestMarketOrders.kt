package exchange.test

import exchange.Market
import exchange.MarketHistory
import exchange.MarketOrders
import exchange.MarketPrices
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class TestMarketOrders(
        private val fromCoin: String,
        private val toCoin: String,
        private val portfolio: TestPortfolio,
        private val prices: MarketPrices
) : MarketOrders {
    private val threadContext = newSingleThreadContext("testMarket")

    override suspend fun buy(amount: BigDecimal) {
        launch(threadContext) {
            delay(50, TimeUnit.MILLISECONDS)
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
    }

    override suspend fun sell(amount: BigDecimal) {
        launch(threadContext) {
            delay(50, TimeUnit.MILLISECONDS)
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
    }
}