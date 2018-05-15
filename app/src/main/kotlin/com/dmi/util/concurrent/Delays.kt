package com.dmi.util.concurrent

import kotlinx.coroutines.experimental.delay
import java.time.Duration
import java.util.concurrent.TimeUnit

suspend fun delay(duration: Duration) {
    delay(duration.toNanos(), TimeUnit.NANOSECONDS)
}

suspend fun safeDelay(duration: Duration) {
    if (duration > Duration.ZERO) {
        delay(duration.toNanos(), TimeUnit.NANOSECONDS)
    }
}