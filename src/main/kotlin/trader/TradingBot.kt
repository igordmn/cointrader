package trader

import exchange.ExchangeTime
import kotlinx.coroutines.experimental.NonCancellable.isActive
import org.slf4j.Logger
import util.concurrent.delay
import util.lang.truncatedTo
import util.log.stackTraceString
import java.time.Duration
import java.time.Instant

class TradingBot(
        private val period: Duration,
        private val time: ExchangeTime,
        private val trade: Trade,
        private val listener: Listener
) {
    suspend fun run() {
        while (isActive) {
            try {
                trade()
            } catch (e: Exception) {
                listener.afterException(e)
            }
        }
    }

    private suspend fun trade() {
        val currentTime = time.current()
        val truncated = currentTime.truncatedTo(period)
        val nextStart = if (truncated == currentTime) truncated else truncated + period
        val timeForStart = Duration.between(currentTime, nextStart)

        listener.beforeDelay(currentTime, timeForStart)
        delay(timeForStart)

        listener.beforePerform()
        trade.perform(nextStart)
        listener.afterPerform(time.current())
    }

    interface Listener {
        fun beforeDelay(currentTime: Instant, delay: Duration) = Unit
        fun beforePerform() = Unit
        fun afterPerform(currentTime: Instant) = Unit
        fun afterException(exception: Exception) = Unit
    }

    class LogListener(private val log: Logger) : Listener {
        override fun beforeDelay(currentTime: Instant, delay: Duration) {
            log.info("beforeDelay   currentTime $currentTime   delay $delay")
        }

        override fun beforePerform() {
            log.info("beforePerform")
        }

        override fun afterPerform(currentTime: Instant) {
            log.info("afterPerform   currentTime $currentTime")
        }

        override fun afterException(exception: Exception) {
            log.error("afterException", exception)
        }
    }
}