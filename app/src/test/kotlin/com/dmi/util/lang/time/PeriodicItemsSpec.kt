package com.dmi.util.lang.time

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.take
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import com.dmi.util.lang.RangeTimed
import com.dmi.util.lang.RangeTimedMerger
import java.time.Duration
import java.time.Instant

class PeriodicItemsSpec : FreeSpec({
    fun instant(millis: Int) = Instant.ofEpochMilli(millis.toLong())
    fun period(millis: Int) = Duration.ofMillis(millis.toLong())

    fun cutInside(range: IntRange, t1: Double, t2: Double): IntRange {
        require(t1 in 0.0..1.0)
        require(t2 in 0.0..1.0)
        require(t1 <= t2)

        val start = range.start + (range.endInclusive - range.start) * t1
        val end = range.start + (range.endInclusive - range.start) * t2
        return IntRange(start.toInt(), end.toInt())
    }

    val merger = object : RangeTimedMerger<IntRange> {
        override fun merge(a: RangeTimed<IntRange>, b: RangeTimed<IntRange>): RangeTimed<IntRange> {
            require(a.timeRange.endInclusive == b.timeRange.start)
            return RangeTimed(a.timeRange.start..b.timeRange.endInclusive, a.item.start..b.item.endInclusive)
        }
    }

    val cutter = RangeTimedCutter(::cutInside)

    val items = listOf(
            RangeTimed(instant(200)..Instant.MAX, 2000..2000),
            RangeTimed(instant(190)..instant(200), 1900..2000),
            RangeTimed(instant(187)..instant(190), 1870..1900),
            RangeTimed(instant(180)..instant(187), 1800..1870),
            RangeTimed(instant(176)..instant(180), 1760..1800),
            RangeTimed(instant(175)..instant(176), 1750..1760),
            RangeTimed(Instant.MIN..instant(175), 1750..1750)
    )

    "periodic items from 210 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(210)).take(5).toList()

            with(periodicItems) {
                size shouldBe 5
                this[0] shouldBe RangeTimed(instant(200)..instant(210), 2000..2000)
                this[1] shouldBe RangeTimed(instant(190)..instant(200), 1900..2000)
                this[2] shouldBe RangeTimed(instant(180)..instant(190), 1800..1900)
                this[3] shouldBe RangeTimed(instant(170)..instant(180), 1750..1800)
                this[4] shouldBe RangeTimed(instant(160)..instant(170), 1750..1750)
            }
        }
    }

    "periodic items from 209 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(209)).take(5).toList()

            with(periodicItems) {
                size shouldBe 5
                this[0] shouldBe RangeTimed(instant(199)..instant(209), 1990..2000)
                this[1] shouldBe RangeTimed(instant(189)..instant(199), 1890..1990)
                this[2] shouldBe RangeTimed(instant(179)..instant(189), 1790..1890)
                this[3] shouldBe RangeTimed(instant(169)..instant(179), 1750..1790)
                this[4] shouldBe RangeTimed(instant(159)..instant(169), 1750..1750)
            }
        }
    }

    "periodic items from 211 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(211)).take(5).toList()

            with(periodicItems) {
                size shouldBe 5
                this[0] shouldBe RangeTimed(instant(201)..instant(211), 2000..2000)
                this[1] shouldBe RangeTimed(instant(191)..instant(201), 1910..2000)
                this[2] shouldBe RangeTimed(instant(181)..instant(191), 1810..1910)
                this[3] shouldBe RangeTimed(instant(171)..instant(181), 1750..1810)
                this[4] shouldBe RangeTimed(instant(161)..instant(171), 1750..1750)
            }
        }
    }

    "periodic items from 211 by 11" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(11))
            val periodicItems = continuouslyCandles.before(instant(211)).take(5).toList()

            with(periodicItems) {
                size shouldBe 5
                this[0] shouldBe RangeTimed(instant(200)..instant(211), 2000..2000)
                this[1] shouldBe RangeTimed(instant(189)..instant(200), 1890..2000)
                this[2] shouldBe RangeTimed(instant(178)..instant(189), 1780..1890)
                this[3] shouldBe RangeTimed(instant(167)..instant(178), 1750..1780)
                this[4] shouldBe RangeTimed(instant(156)..instant(167), 1750..1750)
            }
        }
    }

    "periodic items from 200 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(200)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(190)..instant(200), 1900..2000)
                this[1] shouldBe RangeTimed(instant(180)..instant(190), 1800..1900)
                this[2] shouldBe RangeTimed(instant(170)..instant(180), 1750..1800)
                this[3] shouldBe RangeTimed(instant(160)..instant(170), 1750..1750)
            }
        }
    }

    "periodic items from 201 by 1" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(1))
            val periodicItems = continuouslyCandles.before(instant(201)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(200)..instant(201), 2000..2000)
                this[1] shouldBe RangeTimed(instant(199)..instant(200), 1990..2000)
                this[2] shouldBe RangeTimed(instant(198)..instant(199), 1980..1990)
                this[3] shouldBe RangeTimed(instant(197)..instant(198), 1970..1980)
            }
        }
    }

    "periodic items from 176 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(176)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(166)..instant(176), 1750..1760)
                this[1] shouldBe RangeTimed(instant(156)..instant(166), 1750..1750)
                this[2] shouldBe RangeTimed(instant(146)..instant(156), 1750..1750)
                this[3] shouldBe RangeTimed(instant(136)..instant(146), 1750..1750)
            }
        }
    }

    "periodic items from 175 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(175)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(165)..instant(175), 1750..1750)
                this[1] shouldBe RangeTimed(instant(155)..instant(165), 1750..1750)
                this[2] shouldBe RangeTimed(instant(145)..instant(155), 1750..1750)
                this[3] shouldBe RangeTimed(instant(135)..instant(145), 1750..1750)
            }
        }
    }

    "periodic items from 174 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(174)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(164)..instant(174), 1750..1750)
                this[1] shouldBe RangeTimed(instant(154)..instant(164), 1750..1750)
                this[2] shouldBe RangeTimed(instant(144)..instant(154), 1750..1750)
                this[3] shouldBe RangeTimed(instant(134)..instant(144), 1750..1750)
            }
        }
    }

    "periodic items from 210 by 20" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(20))
            val periodicItems = continuouslyCandles.before(instant(210)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(190)..instant(210), 1900..2000)
                this[1] shouldBe RangeTimed(instant(170)..instant(190), 1750..1900)
                this[2] shouldBe RangeTimed(instant(150)..instant(170), 1750..1750)
                this[3] shouldBe RangeTimed(instant(130)..instant(150), 1750..1750)
            }
        }
    }

    "periodic items from 210 by 100" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(100))
            val periodicItems = continuouslyCandles.before(instant(210)).take(2).toList()

            with(periodicItems) {
                size shouldBe 2
                this[0] shouldBe RangeTimed(instant(110)..instant(210), 1750..2000)
                this[1] shouldBe RangeTimed(instant(10)..instant(110), 1750..1750)
            }
        }
    }
})