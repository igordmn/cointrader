package com.dmi.cointrader.app.broker

import com.dmi.cointrader.app.broker.Broker.OrderError
import com.dmi.cointrader.app.broker.Broker.OrderResult
import com.dmi.cointrader.app.broker.SafeBroker.Attempts
import java.math.BigDecimal
import java.math.RoundingMode

private typealias Order = suspend (newAmount: BigDecimal) -> OrderResult

fun Broker.safe(attempts: Attempts): Broker = SafeBroker(this, attempts)

class SafeBroker(private val original: Broker, private val attempts: Attempts) : Broker {
    override val limits: Broker.Limits = original.limits

    suspend override fun buy(amount: BigDecimal): OrderResult = processAmount(amount) {
        original.buy(it)
    }

    suspend override fun sell(amount: BigDecimal): OrderResult = processAmount(amount) {
        original.sell(it)
    }

    private suspend fun processAmount(amount: BigDecimal, order: Order): OrderResult {
        var attempt = 0
        var currentAmount = amount
        while (true) {
            try {
                return limitAmount(currentAmount, order)
            } catch (e: OrderError.InsufficientBalance) {
                if (attempt == attempts.count - 1) {
                    throw e
                } else {
                    attempt++
                    currentAmount *= attempts.amountMultiplier.toBigDecimal()
                    continue
                }
            }
        }
    }

    private suspend fun limitAmount(amount: BigDecimal, order: Order): OrderResult {
        if (amount < BigDecimal.ZERO) {
            throw OrderError.WrongAmount
        }

        val roundedAmount = if (limits.amountStep > BigDecimal.ZERO) {
            amount.divide(limits.amountStep, 0, RoundingMode.FLOOR) * limits.amountStep
        } else {
            amount
        }

        return if (roundedAmount >= limits.minAmount) {
            order(roundedAmount)
        } else {
            OrderResult(1.0)
        }
    }

    data class Attempts(val count: Int, val amountMultiplier: Double)
}