package exchange

import java.math.BigDecimal

typealias CoinToCandles = Map<String, List<Candle>>

data class Candle(
        val openPrice: BigDecimal,
        val closePrice: BigDecimal,
        val highPrice: BigDecimal,
        val lowPrice: BigDecimal
)