package trader

import exchange.ExchangeTime
import kotlinx.coroutines.experimental.NonCancellable.isActive
import util.concurrent.delay
import util.lang.truncatedTo
import java.time.Duration

class TradingBot(
        private val period: Duration,
        private val time: ExchangeTime,
        private val trade: Trade
) {
    suspend fun run() {
        while (isActive) {
            val currentTime = time.current()
            val truncated = currentTime.truncatedTo(period)
            val nextStart = if (truncated == currentTime) truncated else truncated + period
            val timeForStart = Duration.between(currentTime, nextStart)
            delay(timeForStart)
            trade.perform(nextStart)
        }
    }
}