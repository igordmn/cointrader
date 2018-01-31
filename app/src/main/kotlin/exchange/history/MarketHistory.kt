package exchange.history

import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import java.time.Instant

interface MarketHistory {
    fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle>
}