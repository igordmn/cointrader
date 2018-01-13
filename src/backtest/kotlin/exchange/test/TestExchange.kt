package exchange.test

import exchange.Exchange
import exchange.Markets
import exchange.Portfolio
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

class TestExchange(
        override val portfolio: Portfolio,
        override val markets: Markets,
        private var currentTime: Instant
) : Exchange {
    override suspend fun currentTime(): Instant = suspendCoroutine { continuation ->
        launch {
            delay(50, TimeUnit.MILLISECONDS)
            continuation.resume(currentTime)
        }
    }

    fun setTime(time: Instant) {
        currentTime = time
    }
}