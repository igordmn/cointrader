package old.exchange.candle

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import java.time.Instant

fun ReceiveChannel<TimedCandle>.fillSkipped(): ReceiveChannel<TimedCandle> = produce {
    var previous: TimedCandle? = null

    consumeEach { current ->
        if (previous == null) {
            send(TimedCandle(
                    current.timeRange.endInclusive..Instant.MAX,
                    Candle(
                            current.item.close,
                            current.item.close,
                            current.item.close,
                            current.item.close
                    )
            ))
        } else {
            require(current.timeRange.endInclusive <= previous!!.timeRange.start)

            if (current.timeRange.endInclusive != previous!!.timeRange.start) {
                send(TimedCandle(
                        current.timeRange.endInclusive..previous!!.timeRange.start,
                        Candle(
                                current.item.close,
                                current.item.close,
                                current.item.close,
                                current.item.close
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
                        previous!!.item.open,
                        previous!!.item.open,
                        previous!!.item.open,
                        previous!!.item.open
                )
        ))
    }
}