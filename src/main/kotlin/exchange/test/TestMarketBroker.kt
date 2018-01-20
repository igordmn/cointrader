package exchange.test

import exchange.MarketBroker
import exchange.MarketLimits
import exchange.MarketPrice
import org.slf4j.Logger
import util.math.round
import java.math.BigDecimal
import java.math.RoundingMode

class TestMarketBroker(
        private val fromCoin: String,
        private val toCoin: String,
        private val portfolio: TestPortfolio,
        private val price: MarketPrice,
        private val fee: BigDecimal,
        private val limits: MarketLimits,
        private val listener: Listener
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) {
        val currentPrice = price.current()
        val limits = limits.get()
        val roundedAmount = if (limits.amountStep > BigDecimal.ZERO) {
            amount.divide(limits.amountStep, 0, RoundingMode.FLOOR) * limits.amountStep
        } else {
            amount
        }

        listener.beforeBuy(fromCoin, toCoin, amount, currentPrice, roundedAmount, limits.amountStep, limits.minTotalPrice)

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

        listener.beforeSell(fromCoin, toCoin, amount, currentPrice, roundedAmount, limits.amountStep, limits.minTotalPrice)

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

    interface Listener {
        fun beforeBuy(fromCoin: String, toCoin: String, amount: BigDecimal, currentPrice: BigDecimal, roundedAmount: BigDecimal, amountStep: BigDecimal, minTotalPrice: BigDecimal) = Unit
        fun beforeSell(fromCoin: String, toCoin: String, amount: BigDecimal, currentPrice: BigDecimal, roundedAmount: BigDecimal, amountStep: BigDecimal, minTotalPrice: BigDecimal) = Unit
    }

    class EmptyListener : Listener

    class LogListener(private val log: Logger) : Listener {
        override fun beforeBuy(fromCoin: String, toCoin: String, amount: BigDecimal, currentPrice: BigDecimal, roundedAmount: BigDecimal, amountStep: BigDecimal, minTotalPrice: BigDecimal) {
            val amountR = amount.round(10)
            val currentPriceR = currentPrice.round(10)
            val roundedAmountR = roundedAmount.round(10)
            val amountStepR = amountStep.round(10)
            val minTotalPriceR = minTotalPrice.round(10)
            log.debug("beforeBuy   fromCoin $fromCoin   toCoin $toCoin   amount $amountR   currentPrice $currentPriceR   roundedAmount $roundedAmountR   amountStep $amountStepR   minTotalPrice $minTotalPriceR")
        }

        override fun beforeSell(fromCoin: String, toCoin: String, amount: BigDecimal, currentPrice: BigDecimal, roundedAmount: BigDecimal, amountStep: BigDecimal, minTotalPrice: BigDecimal) {
            val amountR = amount.round(10)
            val currentPriceR = currentPrice.round(10)
            val roundedAmountR = roundedAmount.round(10)
            val amountStepR = amountStep.round(10)
            val minTotalPriceR = minTotalPrice.round(10)
            log.debug("beforeSell   fromCoin $fromCoin   toCoin $toCoin   amount $amountR   currentPrice $currentPriceR   roundedAmount $roundedAmountR   amountStep $amountStepR   minTotalPrice $minTotalPriceR")
        }
    }
}