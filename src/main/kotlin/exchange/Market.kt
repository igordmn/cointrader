package exchange

import java.math.BigDecimal

interface Market {
    val history: MarketHistory
    suspend fun buy(amount: BigDecimal)
    suspend fun sell(amount: BigDecimal)
}