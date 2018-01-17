package exchange.binance.market

import com.binance.api.client.domain.market.Candlestick
import exchange.candle.Candle
import exchange.MarketHistory
import exchange.candle.TimedCandle
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import util.lang.times
import util.lang.truncatedTo
import util.lang.zipWithNext
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import util.math.max
import util.math.min

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
    suspend fun ReceiveChannel<TimedCandle>.fillSkippedReversed(endTime: Instant): ReceiveChannel<TimedCandle> = produce {
        var isFirst = true
        zipWithNext().consumeEach { (current, previous) ->
            require(previous.closedTime <= current.openTime)

            if (isFirst && current.closedTime < endTime) {
                send(TimedCandle(
                        current.closedTime,
                        endTime,
                        Candle(
                                current.candle.close,
                                current.candle.close,
                                current.candle.close,
                                current.candle.close
                        )
                ))
            }

            send(current)

            if (current.openTime != previous.closedTime) {
                send(TimedCandle(
                        previous.closedTime,
                        current.openTime,
                        Candle(
                                previous.candle.close,
                                current.candle.open,
                                max(previous.candle.close, current.candle.open),
                                min(previous.candle.close, current.candle.open)
                        )
                ))
            }

            send(previous)

            isFirst = false
        }

        close()
    }

    suspend fun candlesByMinuteBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
        var timeIt = time

        do {
            val chunk = candlesChunkByMinuteBefore(timeIt)
            chunk.asReversed().forEach {
                send(it)
            }
            timeIt = chunk.first().openTime
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
        val result = api.getCandlestickBars(name, "1m", maxBinanceCount, null, end.toEpochMilli()).await().map(Candlestick::toLocalCandle)

        result.filter { it.closedTime <= end }.asReversed().zipWithNext().forEach { (current, next) ->
            require(current.closedTime <= next.openTime)
        }

        return result
    }
}