package util.lang.time

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.take
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import util.lang.RangeTimed
import util.lang.RangeTimedMerger
import java.time.Duration
import java.time.Instant

class PeriodicItemsSpec : FreeSpec({
    fun instant(millis: Int) = Instant.ofEpochMilli(millis.toLong())
    fun period(millis: Int) = Duration.ofMillis(millis.toLong())

    fun cutInside(range: IntRange, t1: Double, t2: Double): IntRange {
        require(t1 in 0.0..1.0)
        require(t2 in 0.0..1.0)
        require(t1 < t2)

        val start = range.start + (range.endInclusive - range.start) * t1
        val end = range.start + (range.endInclusive - range.start) * t1
        return IntRange(start.toInt(), end.toInt())
    }

    val merger = object : RangeTimedMerger<IntRange> {
        override fun merge(a: RangeTimed<IntRange>, b: RangeTimed<IntRange>): RangeTimed<IntRange> {
            require(a.timeRange.endInclusive == b.timeRange.start)
            return RangeTimed(b.timeRange.start..a.timeRange.endInclusive, b.item.start..a.item.endInclusive)
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
            RangeTimed(Instant.MIN..instant(185), 1850..1850)
    )

    "periodic items from 210 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(210)).take(8).toList()

            with(periodicItems) {
                size shouldBe 5
                this[0] shouldBe RangeTimed(instant(200)..instant(210), 2000..2000)
                this[1] shouldBe RangeTimed(instant(190)..instant(200), 1900..2000)
                this[2] shouldBe RangeTimed(instant(180)..instant(190), 1850..1900)
                this[3] shouldBe RangeTimed(instant(170)..instant(180), 1850..1850)
                this[4] shouldBe RangeTimed(instant(160)..instant(170), 1850..1850)
            }
        }
    }

    "periodic items from 209 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(209)).take(8).toList()

            with(periodicItems) {
                size shouldBe 5
                this[0] shouldBe RangeTimed(instant(199)..instant(209), 1990..2000)
                this[1] shouldBe RangeTimed(instant(189)..instant(199), 1890..1990)
                this[2] shouldBe RangeTimed(instant(179)..instant(189), 1850..1890)
                this[3] shouldBe RangeTimed(instant(169)..instant(179), 1850..1850)
                this[4] shouldBe RangeTimed(instant(159)..instant(169), 1850..1850)
            }
        }
    }

    "periodic items from 211 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(210)).take(8).toList()

            with(periodicItems) {
                size shouldBe 5
                this[0] shouldBe RangeTimed(instant(201)..instant(211), 2000..2000)
                this[1] shouldBe RangeTimed(instant(191)..instant(201), 1910..2000)
                this[2] shouldBe RangeTimed(instant(181)..instant(191), 1850..1910)
                this[3] shouldBe RangeTimed(instant(171)..instant(181), 1850..1850)
                this[4] shouldBe RangeTimed(instant(161)..instant(171), 1850..1850)
            }
        }
    }

    "periodic items from 211 by 11" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(210)).take(8).toList()

            with(periodicItems) {
                size shouldBe 5
                this[0] shouldBe RangeTimed(instant(200)..instant(210), 2000..2000)
                this[1] shouldBe RangeTimed(instant(190)..instant(201), 1900..2000)
                this[2] shouldBe RangeTimed(instant(179)..instant(190), 1850..1900)
                this[3] shouldBe RangeTimed(instant(168)..instant(179), 1850..1850)
                this[4] shouldBe RangeTimed(instant(157)..instant(168), 1850..1850)
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
                this[1] shouldBe RangeTimed(instant(180)..instant(190), 1850..1900)
                this[2] shouldBe RangeTimed(instant(170)..instant(180), 1850..1850)
                this[3] shouldBe RangeTimed(instant(160)..instant(170), 1850..1850)
            }
        }
    }

    "periodic items from 201 by 1" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(200)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(200)..instant(201), 2000..2000)
                this[1] shouldBe RangeTimed(instant(199)..instant(200), 1990..2000)
                this[2] shouldBe RangeTimed(instant(198)..instant(199), 1980..1990)
                this[3] shouldBe RangeTimed(instant(197)..instant(198), 1970..1980)
            }
        }
    }

    "periodic items from 186 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(186)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(176)..instant(186), 1850..1860)
                this[1] shouldBe RangeTimed(instant(166)..instant(176), 1850..1850)
                this[2] shouldBe RangeTimed(instant(156)..instant(166), 1850..1850)
                this[3] shouldBe RangeTimed(instant(146)..instant(176), 1850..1850)
            }
        }
    }

    "periodic items from 185 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(185)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(175)..instant(185), 1850..1850)
                this[1] shouldBe RangeTimed(instant(165)..instant(175), 1850..1850)
                this[2] shouldBe RangeTimed(instant(155)..instant(165), 1850..1850)
                this[3] shouldBe RangeTimed(instant(145)..instant(175), 1850..1850)
            }
        }
    }

    "periodic items from 184 by 10" {
        runBlocking {
            val continuouslyCandles = PeriodicItems(items.asReceiveChannel(), cutter, merger, period(10))
            val periodicItems = continuouslyCandles.before(instant(184)).take(4).toList()

            with(periodicItems) {
                size shouldBe 4
                this[0] shouldBe RangeTimed(instant(174)..instant(184), 1850..1850)
                this[1] shouldBe RangeTimed(instant(164)..instant(174), 1850..1850)
                this[2] shouldBe RangeTimed(instant(154)..instant(164), 1850..1850)
                this[3] shouldBe RangeTimed(instant(144)..instant(174), 1850..1850)
            }
        }
    }
})