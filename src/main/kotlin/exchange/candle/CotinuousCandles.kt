package exchange.candle

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import java.time.Duration
import java.time.Instant

suspend fun ReceiveChannel<TimedCandle>.fillSkipped(): ReceiveChannel<TimedCandle> = produce {
    var previous: TimedCandle? = null

    consumeEach { current ->
        if (previous == null) {
            send(TimedCandle(
                    current.closeTime,
                    Instant.MAX,
                    Candle(
                            current.candle.close,
                            current.candle.close,
                            current.candle.close,
                            current.candle.close
                    )
            ))
        } else {
            require(current.closeTime <= previous!!.openTime)

            if (current.closeTime != previous!!.openTime) {
                send(TimedCandle(
                        current.closeTime,
                        previous!!.openTime,
                        Candle(
                                current.candle.close,
                                current.candle.close,
                                current.candle.close,
                                current.candle.close
                        )
                ))
            }
        }

        send(current)

        previous = current
    }

    if (previous != null) {
        send(TimedCandle(
                Instant.MIN,
                previous!!.openTime,
                Candle(
                        previous!!.candle.open,
                        previous!!.candle.open,
                        previous!!.candle.open,
                        previous!!.candle.open
                )
        ))
    }

    close()
}

class ContinuouslyCandles(
        private val original: ReceiveChannel<TimedCandle>,
        private val approximatedPricesFactory: ApproximatedPricesFactory
) {
    suspend fun before(endTime: Instant, period: Duration): ReceiveChannel<TimedCandle> = produce {
        var closeTime = endTime
        var builder = CombinedCandleReverseBuilder(closeTime - period, closeTime)

        for (candle in original) {
            val completedCandle = builder.build(candle)
            if (completedCandle != null) {
                send(completedCandle!!)
                closeTime -= period
                builder = CombinedCandleReverseBuilder(closeTime - period, closeTime)
            }
        }
    }

    private fun portion(time: Instant, timeRange: ClosedRange<Instant>): Double {
        require(timeRange.endInclusive >= timeRange.start)
        require(time in timeRange)

        return Duration.between(time, end) / Duration.between(start, end)
    }

    private inner class CombinedCandleReverseBuilder(
            private val openTime: Instant,
            private val closeTime: Instant
    ) {
        private val candles = ArrayList<TimedCandle>()

        fun build(candle: TimedCandle): TimedCandle? {
            require(candles.isEmpty() || candles.last().openTime == candle.closeTime)
            require(candle.closeTime > openTime && candle.openTime < closeTime)

            if (candle.openTime < openTime || candle.closeTime > closeTime) {
                val t1 = if (candle.openTime < openTime) portion(openTime, candle.openTime..candle.closeTime) else 0.0
                val t2 = if (candle.closeTime > closeTime) portion(closeTime, candle.openTime..candle.closeTime) else 1.0
                val approximatedPrices = approximatedPricesFactory.forCandle(candle.candle)
                val cutCandle = TimedCandle(openTime, closeTime, approximatedPrices.cutCandle(t1, t2))
                candles.add(cutCandle)
            } else {
                candles.add(candle)
            }

            val isComplete = candles.size > 0 && openTime >= candles.last().openTime && closeTime <= candles.first().closeTime

            return if (isComplete) {
                TimedCandle(
                        openTime,
                        closeTime,
                        Candle(
                                open = candles.first().candle.open,
                                close = candles.last().candle.close,
                                high = candles.maxBy { it.candle.high }!!.candle.high,
                                low = candles.minBy { it.candle.high }!!.candle.low
                        )
                )
            } else {
                null
            }
        }
    }
}