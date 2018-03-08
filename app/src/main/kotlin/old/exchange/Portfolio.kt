package old.exchange

import java.math.BigDecimal

interface Portfolio {
    suspend fun amounts(): Map<String, BigDecimal>
}