package exchange

import java.math.BigDecimal

interface MarketBroker {
    suspend fun buy(amount: BigDecimal)
    suspend fun sell(amount: BigDecimal)

    sealed class Error : Exception() {
        class InsufficientBalance: Error()
        class WrongAmount: Error()
    }
}