package com.dmi.util.lang

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.Temporal
import java.time.temporal.UnsupportedTemporalTypeException

const val HOURS_PER_DAY = 24
const val MINUTES_PER_HOUR = 60
const val MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY
const val SECONDS_PER_MINUTE = 60
const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY
const val MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L
const val MICROS_PER_DAY = SECONDS_PER_DAY * 1000_000L
const val NANOS_PER_SECOND = 1000_000_000L
const val NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE
const val NANOS_PER_HOUR = NANOS_PER_MINUTE * MINUTES_PER_HOUR
const val NANOS_PER_DAY = NANOS_PER_HOUR * HOURS_PER_DAY

typealias InstantRange = ClosedRange<Instant>

fun Instant.truncatedTo(duration: Duration): Instant {
    val dur = duration.toNanos()
    require(duration.seconds <= SECONDS_PER_DAY)
    require(NANOS_PER_DAY % dur == 0L)

    val nod = (epochSecond % SECONDS_PER_DAY) * NANOS_PER_SECOND + nano
    val result = (nod / dur) * dur
    return plusNanos(result - nod)
}

operator fun Duration.times(multiplier: Int): Duration = this.multipliedBy(multiplier.toLong())

fun Duration.toNanosDouble(): Double = NANOS_PER_SECOND * seconds.toDouble() + nano.toDouble()

fun instantRangeOfMilli(startMilli: Long, endMilli: Long): InstantRange {
    return Instant.ofEpochMilli(startMilli)..Instant.ofEpochMilli(endMilli)
}

fun InstantRange.portion(time: Instant): Double {
    require(time in this)
    require(endInclusive > start)
    return Duration.between(start, time).toNanosDouble() / Duration.between(start, endInclusive).toNanosDouble()
}

data class RangeTimed<out T>(val timeRange: InstantRange, val item: T) {
    init {
        require(timeRange.endInclusive > timeRange.start)
    }
}

interface RangeTimedMerger<T> {
    fun mergeNullable(a: RangeTimed<T>?, b: RangeTimed<T>?): RangeTimed<T>? = when {
        a == null -> b
        b == null -> a
        else -> merge(a, b)
    }

    fun merge(a: RangeTimed<T>, b: RangeTimed<T>): RangeTimed<T>
}