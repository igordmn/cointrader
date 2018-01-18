package exchange.candle

import util.math.max
import util.math.min
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
        val timeRange: ClosedRange<Instant>,
        val candle: Candle
) {
    init {
        require(timeRange.endInclusive > timeRange.start)
    }

    infix fun addAfter(other: TimedCandle): TimedCandle {
        require(other.timeRange.start == timeRange.endInclusive)
        return TimedCandle(
                timeRange = timeRange.start..other.timeRange.endInclusive,
                candle = Candle(
                        open = candle.open,
                        close = other.candle.close,
                        high = max(candle.high, other.candle.high),
                        low = min(candle.low, other.candle.low)
                )
        )
    }
}

infix fun TimedCandle?.addAfter(other: TimedCandle): TimedCandle = this?.addAfter(other) ?: other
infix fun TimedCandle?.addBefore(other: TimedCandle): TimedCandle = if (this == null) other else other.addAfter(this)