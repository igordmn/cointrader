package com.dmi.cointrader.app.archive

import com.dmi.util.lang.*
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

@Serializable
data class Period(val num: Int) : Comparable<Period> {
    override fun compareTo(other: Period): Int = num.compareTo(other.num)
    fun next(count: Int = 1): Period = Period(num + count)
    fun previous(count: Int = 1): Period = Period(num - count)
    infix fun until(end: Period): PeriodRange = this..Period(end.num - 1)
}

@Serializable
data class Periods(
        @Serializable(with = InstantSerializer::class) val start: Instant,
        @Serializable(with = DurationSerializer::class) val duration: Duration
) {
    fun of(time: Instant) = Period(numOf(time))

    private fun numOf(time: Instant): Int {
        val distMillis = Duration.between(start, time).toMillis()
        val periodMillis = duration.toMillis()
        return Math.floorDiv(distMillis, periodMillis).toInt()
    }

    fun timeOf(period: Period): Instant {
        return start + duration * period.num
    }

    fun perDay(): Double = MILLIS_PER_DAY / duration.toMillis().toDouble()
    fun perMinute(): Double = MILLIS_PER_MINUTE / duration.toMillis().toDouble()

    operator fun times(value: Int): Periods = Periods(start, duration * value)
}

typealias PeriodRange = ClosedRange<Period>

fun PeriodRange.nums(): IntRange = start.num..endInclusive.num
fun PeriodRange.asSequence(): Sequence<Period> = (start.num..endInclusive.num).asSequence().map(::Period)
fun PeriodRange.size() = endInclusive.num - start.num + 1
fun IntRange.toPeriods(): PeriodRange = Period(start)..Period(endInclusive)

fun periodSequence(start: Period = Period(0)): Sequence<Period> = generateSequence(start) { it.next() }