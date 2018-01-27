package exchange.test

import exchange.ExchangeTime
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

class TestTime(
        var current: Instant
) : ExchangeTime {
    override suspend fun current(): Instant = suspendCoroutine { continuation ->
        launch {
            delay(10, TimeUnit.MILLISECONDS)
            continuation.resume(current)
        }
    }
}