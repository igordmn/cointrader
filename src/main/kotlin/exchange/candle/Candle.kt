package exchange.candle

import java.math.BigDecimal
import java.time.Instant

typealias CoinToCandles = Map<String, List<Candle>>

data class Candle(
        val open: BigDecimal,
        val close: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal
) {
    init {
        require(high >= open)
        require(high >= close)
        require(low <= open)
        require(low <= close)
        require(high >= low)
    }
}

data class TimedCandle(
        val openTime: Instant,
        val closeTime: Instant,
        val candle: Candle
) {
    init {
        require(closeTime >= openTime)
    }
}