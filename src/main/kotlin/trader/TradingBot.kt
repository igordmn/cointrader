package trader

import exchange.Exchange
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
            delay(timeUntilNextPeriod())
            trade.perform()
        }
    }

    private suspend fun timeUntilNextPeriod(): Duration {
        val currentTime = time.current()
        val truncated = currentTime.truncatedTo(period)
        return if (truncated == currentTime) Duration.ZERO else Duration.between(currentTime, truncated + period)
    }
}