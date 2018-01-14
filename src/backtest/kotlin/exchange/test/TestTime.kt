package exchange.test

import exchange.ExchangeTime
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

class TestTime(
        private var current: Instant
) : ExchangeTime {
    override suspend fun current(): Instant = suspendCoroutine { continuation ->
        launch {
            delay(50, TimeUnit.MILLISECONDS)
            continuation.resume(current)
        }
    }

    fun setCurrent(time: Instant) {
        current = time
    }
}