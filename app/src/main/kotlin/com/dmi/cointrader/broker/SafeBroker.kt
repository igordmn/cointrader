package com.dmi.cointrader.broker

import com.dmi.cointrader.broker.Broker.OrderError
import com.dmi.cointrader.broker.Broker.OrderResult
import com.dmi.cointrader.broker.SafeBroker.Attempts
import com.dmi.util.lang.retry
import java.lang.Math.pow
import java.math.BigDecimal
import java.math.RoundingMode

private typealias Order = suspend (newAmount: BigDecimal) -> OrderResult

fun Broker.safe(attempts: Attempts): Broker = SafeBroker(this, attempts)

class SafeBroker(private val original: Broker, private val attempts: Attempts) : Broker {
    override val limits: Broker.Limits = original.limits

    override suspend fun buy(amount: BigDecimal): OrderResult = processAmount(amount) {
        original.buy(it)
    }

    override suspend fun sell(amount: BigDecimal): OrderResult = processAmount(amount) {
        original.sell(it)
    }

    private suspend fun processAmount(amount: BigDecimal, order: Order): OrderResult {
        return retry<OrderResult, OrderError.InsufficientBalance>(attempts.count) {
            limitAmount(amount * pow(attempts.amountMultiplier, it.toDouble()).toBigDecimal(), order)
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