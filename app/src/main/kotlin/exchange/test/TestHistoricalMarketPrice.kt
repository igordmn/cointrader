package exchange.test

import exchange.*
import exchange.candle.ApproximatedPricesFactory
import exchange.candle.Candle
import kotlinx.coroutines.experimental.delay
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class TestHistoricalMarketPrice(
        private val time: ExchangeTime,
        private val history: MarketHistory,
        private val approximatedPricesFactory: ApproximatedPricesFactory
) : MarketPrice {
    override suspend fun current(): BigDecimal {
        delay(10, TimeUnit.MILLISECONDS)
        val nextMinute = time.current().truncatedTo(ChronoUnit.MINUTES) + Duration.ofMinutes(1)
        val candle = history.candlesBefore(nextMinute, 1, Duration.ofMinutes(1)).first()
        return randomPriceIn(candle)
    }

    private fun randomPriceIn(candle: Candle): BigDecimal = approximatedPricesFactory.forCandle(candle).exactAt(Math.random())
}