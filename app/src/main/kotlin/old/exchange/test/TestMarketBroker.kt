package old.exchange.test

import old.exchange.MarketBroker
import old.exchange.MarketLimits
import old.exchange.MarketPrice
import org.slf4j.Logger
import com.dmi.util.math.equalsWithoutScale
import com.dmi.util.math.notEqualsWithoutScale
import com.dmi.util.math.round
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
        checkAmount(amount)
        val currentPrice = price.current()
        listener.beforeBuy(fromCoin, toCoin, amount, currentPrice)

        portfolio.modify {
            val fromAmount = it[fromCoin]
            val fromSellAmount = amount * currentPrice
            if (fromSellAmount <= fromAmount) {
                it[fromCoin] = it[fromCoin] - fromSellAmount
                it[toCoin] = it[toCoin] + amount * (BigDecimal.ONE - fee)
            } else {
                throw MarketBroker.Error.InsufficientBalance()
            }
        }
    }

    override suspend fun sell(amount: BigDecimal) {
        checkAmount(amount)
        val currentPrice = price.current()
        listener.beforeSell(fromCoin, toCoin, amount, currentPrice)

        portfolio.modify {
            val toAmount = it[toCoin]
            if (amount <= toAmount) {
                it[fromCoin] = it[fromCoin] + amount * currentPrice * (BigDecimal.ONE - fee)
                it[toCoin] = it[toCoin] - amount
            } else {
                throw MarketBroker.Error.InsufficientBalance()
            }
        }
    }

    private fun checkAmount(amount: BigDecimal) {
        val limits = limits.get()
        listener.beforeCheckAmount(fromCoin, toCoin, amount, limits)
        val tooSmall = amount <= BigDecimal.ZERO || amount < limits.minAmount
        val notMultiplyOfStep = limits.amountStep != BigDecimal.ZERO && (amount % limits.amountStep) notEqualsWithoutScale BigDecimal.ZERO
        if (tooSmall || notMultiplyOfStep) {
            throw MarketBroker.Error.WrongAmount()
        }
    }

    interface Listener {
        fun beforeCheckAmount(fromCoin: String, toCoin: String, amount: BigDecimal, limits: MarketLimits.Value) = Unit
        fun beforeBuy(fromCoin: String, toCoin: String, amount: BigDecimal, currentPrice: BigDecimal) = Unit
        fun beforeSell(fromCoin: String, toCoin: String, amount: BigDecimal, currentPrice: BigDecimal) = Unit
    }

    class EmptyListener : Listener

    class LogListener(private val log: Logger) : Listener {
        override fun beforeCheckAmount(fromCoin: String, toCoin: String, amount: BigDecimal, limits: MarketLimits.Value) {
            val amountR = amount.round(10)
            val minAmountR = limits.minAmount.round(10)
            val amountStepR = limits.amountStep.round(10)
            log.debug("beforeCheckAmount   fromCoin $fromCoin   toCoin $toCoin   amount $amountR   minAmount $minAmountR   amountStep $amountStepR")
        }

        override fun beforeBuy(fromCoin: String, toCoin: String, amount: BigDecimal, currentPrice: BigDecimal) {
            val amountR = amount.round(10)
            val currentPriceR = currentPrice.round(10)
            log.debug("beforeBuy   fromCoin $fromCoin   toCoin $toCoin   amount $amountR   currentPrice $currentPriceR")
        }

        override fun beforeSell(fromCoin: String, toCoin: String, amount: BigDecimal, currentPrice: BigDecimal) {
            val amountR = amount.round(10)
            val currentPriceR = currentPrice.round(10)
            log.debug("beforeSell   fromCoin $fromCoin   toCoin $toCoin   amount $amountR   currentPrice $currentPriceR")
        }
    }
}