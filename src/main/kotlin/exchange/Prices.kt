package exchange

import java.math.BigDecimal

interface Prices {
    suspend fun current(): BigDecimal
}