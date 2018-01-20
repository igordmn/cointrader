package exchange

import java.math.BigDecimal

interface MarketBroker {
    suspend fun buy(amount: BigDecimal)
    suspend fun sell(amount: BigDecimal)
}