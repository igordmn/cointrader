package exchange

import java.math.BigDecimal

interface Portfolio {
    suspend fun amount(coin: String): BigDecimal
}