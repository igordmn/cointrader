package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import exchange.Candle
import exchange.MarketHistory
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

        val endOfPeriod = time.truncatedTo(period).epochSecond - 1
        client.getCandlestickBars(name, period.toServerInterval(), count, null, endOfPeriod) { result ->
            require(result.size == count)
            require(result.last().closeTime == endOfPeriod)
            continuation.resume(result.map(Candlestick::toLocalCandle))
        }
    }
}