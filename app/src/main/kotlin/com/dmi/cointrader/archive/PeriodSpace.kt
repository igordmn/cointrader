package com.dmi.cointrader.archive

import com.dmi.util.lang.*
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import com.dmi.util.lang.minus
import com.dmi.util.math.ceilDiv
import com.dmi.util.math.floorDiv

typealias Period = Int
typealias PeriodRange = IntRange
typealias PeriodProgression = IntProgression

@Serializable
data class PeriodSpace(
        @Serializable(with = InstantSerializer::class) val start: Instant,
        @Serializable(with = DurationSerializer::class) val duration: Duration
) {
    fun floor(time: Instant): Period {
        val distMillis = (time - start).toMillis()
        val periodMillis = duration.toMillis()
        return (distMillis floorDiv periodMillis).toInt()
    }

    fun ceil(time: Instant): Period {
        val distMillis = (time - start).toMillis()
        val periodMillis = duration.toMillis()
        return (distMillis ceilDiv periodMillis).toInt()
    }

    fun timeOf(period: Period): Instant {
        return start + duration * period
    }

    fun periodsPerDay(): Double = MILLIS_PER_DAY / duration.toMillis().toDouble()
    fun periodsPerMinute(): Double = MILLIS_PER_MINUTE / duration.toMillis().toDouble()

    operator fun times(value: Int): PeriodSpace = PeriodSpace(start, duration * value)
}

fun periodSequence(start: Period = 0): Sequence<Period> = generateSequence(start) { it + 1 }

fun ClosedRange<Instant>.periods(space: PeriodSpace): PeriodRange = space.ceil(start)..space.ceil(endInclusive)