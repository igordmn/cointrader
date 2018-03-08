package old.exchange.test

import old.exchange.*
import old.exchange.candle.ApproximatedPricesFactory
import old.exchange.candle.Candle
import old.exchange.history.MarketHistory
import kotlinx.coroutines.experimental.channels.first
import kotlinx.coroutines.experimental.channels.take
import kotlinx.coroutines.experimental.delay
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class TestHistoricalMarketPrice(
        private val time: ExchangeTime,
        private val oneMinuteHistory: MarketHistory,
        private val approximatedPricesFactory: ApproximatedPricesFactory
) : MarketPrice {
    override suspend fun current(): BigDecimal {
        val nextMinute = time.current().truncatedTo(ChronoUnit.MINUTES) + Duration.ofMinutes(1)
        val candle = oneMinuteHistory.candlesBefore(nextMinute).take(1).first().item
//        return randomPriceIn(candle)
        return candle.low
    }

    private fun randomPriceIn(candle: Candle): BigDecimal = approximatedPricesFactory.forCandle(candle).exactAt(Math.random())
}