package com.dmi.util.lang.time

import com.dmi.util.lang.*

class RangeTimedCutter<T>(
        private val cutInside: (T, t1: Double, t2: Double) -> T
) {
    fun cut(rangeTimed: RangeTimed<T>, timeRange: InstantRange): RangeTimed<T>? = when {
        timeRange.endInclusive <= rangeTimed.timeRange.start -> null
        timeRange.start >= rangeTimed.timeRange.endInclusive -> null
        else -> {
            val start = max(timeRange.start, rangeTimed.timeRange.start)
            val end = min(timeRange.endInclusive, rangeTimed.timeRange.endInclusive)
            val t1 = rangeTimed.timeRange.portion(start)
            val t2 = rangeTimed.timeRange.portion(end)
            val cutItem = cutInside(rangeTimed.item, t1, t2)
            RangeTimed(start..end, cutItem)
        }
    }
}