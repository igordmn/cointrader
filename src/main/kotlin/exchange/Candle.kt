package exchange

import java.math.BigDecimal
import java.time.Instant

typealias CoinToCandles = Map<String, List<Candle>>

data class Candle(
        val open: BigDecimal,
        val close: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal
)

data class TimedCandle(
        val openTime: Instant,
        val closedTime: Instant,
        val candle: Candle
)