package exchange.history

import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import java.time.Duration
import java.time.Instant

interface MarketHistory {
    suspend fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle>
}