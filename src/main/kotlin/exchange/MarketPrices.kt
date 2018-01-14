package exchange

import java.math.BigDecimal

interface MarketPrices {
    suspend fun current(): BigDecimal
}