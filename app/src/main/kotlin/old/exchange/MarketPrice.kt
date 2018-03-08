package old.exchange

import java.math.BigDecimal

interface MarketPrice {
    suspend fun current(): BigDecimal
}