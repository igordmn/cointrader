package util.lang

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

fun Instant.truncatedTo(duration: Duration): Instant {
    val dur = duration.toNanos()
    require(duration.seconds <= SECONDS_PER_DAY)
    require(NANOS_PER_DAY % dur == 0L)

    val nod = (epochSecond % SECONDS_PER_DAY) * NANOS_PER_SECOND + nano
    val result = (nod / dur) * dur
    return plusNanos(result - nod)
}

operator fun Duration.times(multiplier: Int): Duration = this.multipliedBy(multiplier.toLong())

fun instantRangeOfMilli(startMilli: Long, endMilli: Long): ClosedRange<Instant> {
    return Instant.ofEpochMilli(startMilli)..Instant.ofEpochMilli(endMilli)
}

infix fun ClosedRange<Instant>.intersects(other: ClosedRange<Instant>): Boolean {
    return endInclusive > other.start || other.endInclusive > start
}

fun ClosedRange<Instant>.portion(time: Instant): Double {
    require(timeRange.endInclusive >= timeRange.start)
    require(time in timeRange)

    return Duration.between(time, end) / Duration.between(start, end)
}