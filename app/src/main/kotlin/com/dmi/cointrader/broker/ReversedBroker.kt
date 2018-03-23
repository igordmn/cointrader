package com.dmi.cointrader.broker

import java.math.BigDecimal
import java.math.RoundingMode

fun Broker.reversed(price: BigDecimal): Broker = ReversedBroker(this, price)

class ReversedBroker(
        private val original: Broker,
        private val price: BigDecimal,
        private val operationScale: Int = 32
) : Broker {
    override val limits: Broker.Limits = original.limits

    override suspend fun buy(amount: BigDecimal) = original.sell(amount.divide(price, operationScale, RoundingMode.DOWN))
    override suspend fun sell(amount: BigDecimal) = original.buy(amount.divide(price, operationScale, RoundingMode.DOWN))
}