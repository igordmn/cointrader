package com.dmi.cointrader.broker

import java.math.BigDecimal

interface Broker {
    val limits: Limits
    suspend fun buy(amount: BigDecimal): OrderResult
    suspend fun sell(amount: BigDecimal): OrderResult

    data class Limits(val minAmount: BigDecimal, val amountStep: BigDecimal)

    data class OrderResult(
            val price: Double
    )

    sealed class OrderError : Exception() {
        object InsufficientBalance : Error()
        object WrongAmount : Error()
    }
}