package trader

import exchange.ExchangeTime
import kotlinx.coroutines.experimental.NonCancellable.isActive
import org.slf4j.Logger
import com.dmi.util.concurrent.delay
import com.dmi.util.lang.truncatedTo
import java.awt.Toolkit
import java.time.Duration
import java.time.Instant

// todo refetch market limits each step
class TradingBot(
        private val period: Duration,
        private val time: ExchangeTime,
        private val trade: Trade,
        private val listener: Listener,
        private val preloadInfo: suspend (Instant) -> Unit,
        private val postloadInfo: suspend () -> Unit
) {
    suspend fun run() {
        while (isActive) {
            try {
                trade()
                postloadInfo()
            } catch (e: Exception) {
                listener.afterException(e)
            }
        }
    }

    private suspend fun trade() {
        val currentTime = time.current()
        val truncated = currentTime.truncatedTo(period) // todo нужно округлять, а не truncate
        val nextStart = if (truncated == currentTime) truncated else truncated + period
        val timeForStart = Duration.between(currentTime, nextStart)

        listener.beforeDelay(currentTime, timeForStart)
        delay(timeForStart)

        listener.beforePerform()
        preloadInfo(nextStart)
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
            log.debug("afterPerform   currentTime $currentTime")
        }

        override fun afterException(exception: Exception) {
            Toolkit.getDefaultToolkit().beep() // todo перенести отсюда
            log.error("afterException", exception)
        }
    }
}