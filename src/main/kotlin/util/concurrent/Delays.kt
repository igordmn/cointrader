package util.concurrent

import kotlinx.coroutines.experimental.delay
import java.time.Duration
import java.util.concurrent.TimeUnit

suspend fun delay(duration: Duration) {
    delay(duration.toNanos(), TimeUnit.NANOSECONDS)
}