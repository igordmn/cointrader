package exchange

import java.math.BigDecimal

typealias CoinToCandles = Map<String, List<Candle>>

data class Candle(
        val open: BigDecimal,
        val close: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal
)