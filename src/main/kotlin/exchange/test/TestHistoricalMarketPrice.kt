package exchange.test

import exchange.Candle
import exchange.ExchangeTime
import exchange.MarketHistory
import exchange.MarketPrice
import kotlinx.coroutines.experimental.delay
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class TestHistoricalMarketPrice(
        private val time: ExchangeTime,
        private val history: MarketHistory
) : MarketPrice {
    override suspend fun current(): BigDecimal {
        delay(10, TimeUnit.MILLISECONDS)
        val nextMinute = time.current().truncatedTo(ChronoUnit.MINUTES) + Duration.ofMinutes(1)
        val candle = history.candlesBefore(nextMinute, 1, Duration.ofMinutes(1)).last()
        return randomPriceIn(candle)
    }

    private fun randomPriceIn(candle: Candle): BigDecimal = approximatePrice(candle, Math.random())

    private fun approximatePrice(candle: Candle, t: Double): BigDecimal {
        fun value(x1: Double, x2: Double, y1: BigDecimal, y2: BigDecimal, x: Double) = y1 + (y2 - y1) / BigDecimal((x2 - x1) * (x - x1))

        return if ((candle.open - candle.high).abs() <= (candle.open - candle.low).abs()) {
            // chart is open-high-low-close
            when {
                t < 1 / 3 -> value(0.0, 1.0 / 3, candle.open, candle.high, t)
                1.0 / 3 <= t && t < 2.0 / 3 -> value(1.0 / 3, 2.0 / 3, candle.high, candle.low, t)
                else -> value(2.0 / 3, 1.0, candle.low, candle.close, t)
            }
        } else {
            // chart is open-low-high-close
            when {
                t < 1 / 3 -> value(0.0, 1.0 / 3, candle.open, candle.low, t)
                1.0 / 3 <= t && t < 2.0 / 3 -> value(1.0 / 3, 2.0 / 3, candle.low, candle.high, t)
                else -> value(2.0 / 3, 1.0, candle.high, candle.close, t)
            }
        }
    }
}