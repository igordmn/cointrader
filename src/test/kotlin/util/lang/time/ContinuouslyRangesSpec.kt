package util.lang.time

import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.runBlocking
import util.lang.RangeTimed
import util.lang.RangeTimedMerger
import java.time.Duration
import java.time.Instant

class RangeTimedCutterSpec : FreeSpec({
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

    "continuously items" {
        runBlocking {
            val items = listOf(
                    RangeTimed(instant(200)..Instant.MAX, 200..200),
                    RangeTimed(instant(190)..instant(200), 190..200),
                    RangeTimed(instant(187)..instant(190), 187..190),
                    RangeTimed(instant(180)..instant(187), 180..187),
                    RangeTimed(instant(176)..instant(180), 176..180),
                    RangeTimed(instant(175)..instant(176), 175..176),
                    RangeTimed(Instant.MIN..instant(185), 185..185)
            )

            val continuouslyCandles = ContinuouslyRanges<IntRange>(items.asReceiveChannel(), cutter, merger, period(10))
        }
    }
})