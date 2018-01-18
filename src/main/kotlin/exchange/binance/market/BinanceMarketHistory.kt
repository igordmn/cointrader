package exchange.binance.market

import com.binance.api.client.domain.market.Candlestick
import exchange.MarketHistory
import exchange.binance.api.BinanceAPI
import exchange.candle.Candle
import exchange.candle.CandleNormalizer
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.*
import util.lang.instantRangeOfMilli
import util.lang.times
import util.lang.truncatedTo
import util.lang.unsupportedOperation
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class BinanceMarketHistory(
        private val name: String,
        private val api: BinanceAPI,
        private val normalizer: CandleNormalizer
) : MarketHistory {
    override suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> {
        val candles = candlesByMinuteBefore(time)
        val normilized = normalizer.normalizeBefore(candles, time, period)
        return normilized.map { it.item }.take(count).toList()
    }

    private fun candlesByMinuteBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
        var timeIt = time

        do {
            val chunk = candlesChunkByMinuteBefore(timeIt)
            chunk.forEach {
                send(it)
            }
            timeIt = chunk.last().timeRange.start
        } while (chunk.isNotEmpty())

        close()
    }

    private suspend fun candlesChunkByMinuteBefore(time: Instant): List<TimedCandle> {
        fun Candlestick.toLocalCandle() = TimedCandle(
                instantRangeOfMilli(openTime, closeTime),
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
                .filter { it.timeRange.endInclusive <= end }
                .filter { it.timeRange.endInclusive > it.timeRange.start }
                .toList()
                .asReversed()

        result.zipWithNext().forEach { (current, next) ->
            require(current.timeRange.endInclusive <= next.timeRange.start)
        }

        return result
    }
}