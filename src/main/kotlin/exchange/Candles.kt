package exchange

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import util.lang.zipWithNext
import util.math.max
import util.math.min
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

fun Candle.approximate(t: Double, operationScale: Int): BigDecimal {
    require(t in 0.0..1.0)

    infix fun BigDecimal.localDivide(other: BigDecimal) = divide(other, operationScale, RoundingMode.HALF_UP)
    fun value(x1: Double, x2: Double, y1: BigDecimal, y2: BigDecimal, x: Double): BigDecimal {
        val result = y1 + ((y2 - y1) * BigDecimal(x - x1) localDivide BigDecimal(x2 - x1))
        return result.setScale(operationScale, RoundingMode.HALF_UP)
    }

    val t1 = 0.0
    val t2 = 1.0 / 3
    val t3 = 2.0 / 3
    val t4 = 1.0

    return when (t) {
        0.0 -> open.setScale(operationScale, RoundingMode.HALF_UP)
        1.0 -> close.setScale(operationScale, RoundingMode.HALF_UP)
        else -> if ((open - high).abs() <= (open - low).abs()) {
            // chart is open-high-low-close
            when {
                t < t2 -> value(t1, t2, open, high, t)
                t2 <= t && t < t3 -> value(t2, t3, high, low, t)
                else -> value(t3, t4, low, close, t)
            }
        } else {
            // chart is open-low-high-close
            when {
                t < t2 -> value(t1, t2, open, low, t)
                t2 <= t && t < t3 -> value(t2, t3, low, high, t)
                else -> value(t3, t4, high, close, t)
            }
        }
    }
}

suspend fun ReceiveChannel<TimedCandle>.continuouslyBefore(endTime: Instant, period: Duration): ReceiveChannel<TimedCandle> = produce {
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

    while (true) {

    }
}

suspend fun ReceiveChannel<TimedCandle>.sample(period: Duration): ReceiveChannel<TimedCandle> {
TODO()
}
