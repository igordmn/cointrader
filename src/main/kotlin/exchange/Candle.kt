package exchange

import java.math.BigDecimal

data class Candle(
        val openPrice: BigDecimal,
        val closePrice: BigDecimal,
        val highPrice: BigDecimal,
        val lowPrice: BigDecimal
)