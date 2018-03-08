package old.exchange.test

import old.exchange.ExchangeTime
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
            continuation.resume(current)
        }
    }
}