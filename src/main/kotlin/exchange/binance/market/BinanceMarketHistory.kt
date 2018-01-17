package exchange.binance.market

import com.binance.api.client.domain.market.Candlestick
import exchange.MarketHistory
import exchange.binance.api.BinanceAPI
import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import util.lang.times
import util.lang.truncatedTo
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class BinanceMarketHistory(
        private val name: String,
        private val api: BinanceAPI
) : MarketHistory {
    override suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> {
        fun Duration.toServerInterval() = when (this) {
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
        result.zipWithNext { a, b ->
            require(a.closeTime + 1 == b.openTime)
        }
        require(result.first().openTime == startMillis)
        require(result.last().closeTime == endMillis)

        return result.map(Candlestick::toLocalCandle)
    }
}

class BinanceMarketHistory2(
        private val name: String,
        private val api: BinanceAPI
) {
    suspend fun candlesByMinuteBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
        var timeIt = time

        do {
            val chunk = candlesChunkByMinuteBefore(timeIt)
            chunk.forEach {
                send(it)
            }
            timeIt = chunk.last().openTime
        } while (chunk.isNotEmpty())

        close()
    }

    private suspend fun candlesChunkByMinuteBefore(time: Instant): List<TimedCandle> {
        fun Candlestick.toLocalCandle() = TimedCandle(
                Instant.ofEpochMilli(openTime),
                Instant.ofEpochMilli(closeTime),
                Candle(
                        BigDecimal(open),
                        BigDecimal(close),
                        BigDecimal(high),
                        BigDecimal(low)
                )
        )

        val maxBinanceCount = 500
        val end = time.truncatedTo(ChronoUnit.MINUTES) - Duration.ofMillis(1)
        val result = api
                .getCandlestickBars(name, "1m", maxBinanceCount, null, end.toEpochMilli())
                .await()
                .asSequence()
                .map(Candlestick::toLocalCandle)
                .filter { it.closeTime <= end }
                .filter { it.closeTime > it.openTime }
                .toList()
                .asReversed()

        result.zipWithNext().forEach { (current, next) ->
            require(current.closeTime <= next.openTime)
        }

        return result
    }
}