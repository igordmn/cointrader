package exchange.binance.market

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import exchange.Candle
import exchange.MarketHistory
import exchange.binance.api.BinanceAPI
import util.lang.times
import util.lang.truncatedTo
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceMarketHistory(
        private val name: String,
        private val api: BinanceAPI
) : MarketHistory {
    override suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> {
        fun Duration.toServerInterval() = when(this) {
            Duration.ofMinutes(1) -> "1m"
            Duration.ofMinutes(5) -> "5m"
            Duration.ofMinutes(15) -> "15m"
            Duration.ofMinutes(30) -> "30m"
            Duration.ofMinutes(60) -> "1h"
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

        val result = api.getCandlestickBars(name, period.toServerInterval(), count, null, endMillis).await()
        require(result.size == count)
        require(result.first().openTime == startMillis)
        require(result.last().closeTime == endMillis)

        return result.map(Candlestick::toLocalCandle)
    }
}