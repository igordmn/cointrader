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


suspend fun ReceiveChannel<TimedCandle>.continuouslyBefore(endTime: Instant, period: Duration): ReceiveChannel<TimedCandle> = produce {
    TODO()
}

suspend fun ReceiveChannel<TimedCandle>.sample(period: Duration): ReceiveChannel<TimedCandle> {
TODO()
}