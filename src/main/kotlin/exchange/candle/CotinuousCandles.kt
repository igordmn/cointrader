package exchange.candle

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import util.lang.max
import util.lang.min
import util.lang.portion
import java.time.Duration
import java.time.Instant

suspend fun ReceiveChannel<TimedCandle>.fillSkipped(): ReceiveChannel<TimedCandle> = produce {
    var previous: TimedCandle? = null

    consumeEach { current ->
        if (previous == null) {
            send(TimedCandle(
                    current.timeRange.endInclusive..Instant.MAX,
                    Candle(
                            current.candle.close,
                            current.candle.close,
                            current.candle.close,
                            current.candle.close
                    )
            ))
        } else {
            require(current.timeRange.endInclusive <= previous!!.timeRange.start)

            if (current.timeRange.endInclusive != previous!!.timeRange.start) {
                send(TimedCandle(
                        current.timeRange.endInclusive..previous!!.timeRange.start,
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
                Instant.MIN..previous!!.timeRange.start,
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

class TimedCandleCutter(
        private val cutInsideCandle: (Candle, t1: Double, t2: Double) -> Candle
) {
    fun cut(timedCandle: TimedCandle, timeRange: ClosedRange<Instant>): TimedCandle? = when {
        timeRange.endInclusive <= timedCandle.timeRange.start -> null
        timeRange.start >= timedCandle.timeRange.endInclusive -> null
        else -> {
            val start = max(timeRange.start, timedCandle.timeRange.start)
            val end = min(timeRange.endInclusive, timedCandle.timeRange.endInclusive)
            val t1 = timedCandle.timeRange.portion(start)
            val t2 = timedCandle.timeRange.portion(end)
            val candle = cutInsideCandle(timedCandle.candle, t1, t2)
            TimedCandle(start..end, candle)
        }
    }
}

class ContinuouslyCandles(
        private val original: ReceiveChannel<TimedCandle>,
        private val cutter: TimedCandleCutter,
        private val period: Duration
) {
    suspend fun before(endTime: Instant): ReceiveChannel<TimedCandle> = produce<TimedCandle> {
        var closeTime = endTime
        var combinedCandle: TimedCandle? = null

        var remainder: TimedCandle? = null
        val it = original.iterator()
        while (remainder != null || it.hasNext()) {
            val candle = remainder ?: it.next()
            val timeRange = closeTime - period..closeTime

            when {
                candle.timeRange.endInclusive <= timeRange.start -> {
                    closeTime -= period
                    combinedCandle = null
                    remainder = candle
                }
                candle.timeRange.start < timeRange.endInclusive -> {
                    val leftCandle = cutter.cut(candle, Instant.MIN..timeRange.start)
                    val insideCandle = cutter.cut(candle, timeRange)

                    if (insideCandle != null) {
                        combinedCandle = combinedCandle.addBefore(insideCandle)
                    }

                    if (combinedCandle != null && combinedCandle.timeRange == timeRange) {
                        if (combinedCandle.timeRange == timeRange) {
                            send(combinedCandle)
                            combinedCandle = null
                            closeTime -= period
                        }
                    }

                    remainder = leftCandle
                }
            }
        }
    }
}