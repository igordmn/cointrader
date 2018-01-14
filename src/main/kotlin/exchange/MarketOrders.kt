package exchange

import java.math.BigDecimal

interface MarketOrders {
    suspend fun buy(amount: BigDecimal)
    suspend fun sell(amount: BigDecimal)
}