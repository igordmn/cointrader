package com.dmi.util.test

import java.time.Duration
import java.time.Instant

fun instant(millis: Long): Instant = Instant.ofEpochMilli(millis)
fun period(millis: Long): Duration = Duration.ofMillis(millis)