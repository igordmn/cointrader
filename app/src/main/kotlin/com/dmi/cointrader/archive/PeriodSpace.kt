package com.dmi.cointrader.archive

import com.dmi.util.lang.*
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import com.dmi.util.lang.minus

typealias Period = Int
typealias PeriodRange = IntRange
typealias PeriodProgression = IntProgression

@Serializable
data class PeriodSpace(
        @Serializable(with = InstantSerializer::class) val start: Instant,
        @Serializable(with = DurationSerializer::class) val duration: Duration
) {
    fun of(time: Instant): Period {
        val distMillis = (time - start).toMillis()
        val periodMillis = duration.toMillis()
        return Math.floorDiv(distMillis, periodMillis).toInt()
    }

    fun timeOf(period: Period): Instant {
        return start + duration * period
    }

    fun perDay(): Double = MILLIS_PER_DAY / duration.toMillis().toDouble()
    fun perMinute(): Double = MILLIS_PER_MINUTE / duration.toMillis().toDouble()

    operator fun times(value: Int): PeriodSpace = PeriodSpace(start, duration * value)
}

fun periodSequence(start: Period = 0): Sequence<Period> = generateSequence(start) { it + 1 }