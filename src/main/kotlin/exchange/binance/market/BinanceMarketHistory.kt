package exchange.binance.market

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import exchange.Candle
import exchange.MarketHistory
import util.lang.times
import util.lang.truncatedTo
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceMarketHistory(
        private val name: String,
        private val client: BinanceApiAsyncRestClient
) : MarketHistory {
    override suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> = suspendCoroutine { continuation ->
        fun Duration.toServerInterval() = when(this) {
            Duration.ofMinutes(1) -> CandlestickInterval.ONE_MINUTE
            Duration.ofMinutes(5) -> CandlestickInterval.FIVE_MINUTES
            Duration.ofMinutes(15) -> CandlestickInterval.FIFTEEN_MINUTES
            Duration.ofMinutes(30) -> CandlestickInterval.HALF_HOURLY
            Duration.ofMinutes(60) -> CandlestickInterval.HOURLY
            else -> throw UnsupportedOperationException()
        }

        fun Candlestick.toLocalCandle() = Candle(
                BigDecimal(close),
                BigDecimal(open),
                BigDecimal(high),
                BigDecimal(low)
        )

        val start = time.truncatedTo(period) - (period * count)
        val end = time.truncatedTo(period) - Duration.ofMillis(1)
        val startMillis = start.toEpochMilli()
        val endMillis = end.toEpochMilli()

        client.getCandlestickBars(name, period.toServerInterval(), count, null, endMillis) { result ->
            require(result.size == count)
            require(result.first().openTime == startMillis)
            require(result.last().closeTime == endMillis)
            continuation.resume(result.map(Candlestick::toLocalCandle))
        }
    }
}