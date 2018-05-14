package com.dmi.cointrader.broker

import java.math.BigDecimal

fun Broker.reversed(price: BigDecimal): Broker = ReversedBroker(this, price)

class ReversedBroker(
        private val original: Broker,
        private val price: BigDecimal
) : Broker {
    override val limits: Broker.Limits = original.limits
    override suspend fun buy(amount: BigDecimal) = original.sell(amount * price)
    override suspend fun sell(amount: BigDecimal) = original.buy(amount * price)
}