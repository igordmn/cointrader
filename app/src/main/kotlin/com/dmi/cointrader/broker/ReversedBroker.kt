package com.dmi.cointrader.broker

import java.math.BigDecimal

fun Broker.reversed(price: BigDecimal): Broker = ReversedBroker(this, price)

class ReversedBroker(
        private val original: Broker,
        private val price: BigDecimal
) : Broker {
    override val limits: Broker.Limits = original.limits

    override suspend fun buy(amount: BigDecimal): Broker.OrderResult {
        val result = original.sell(amount * price)
        return Broker.OrderResult(price = 1.0 / result.price)
    }

    override suspend fun sell(amount: BigDecimal): Broker.OrderResult {
        val result = original.buy(amount * price)
        return Broker.OrderResult(price = 1.0 / result.price)
    }
}