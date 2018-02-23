package com.dmi.util.lang.time

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import com.dmi.util.lang.RangeTimed
import com.dmi.util.lang.RangeTimedMerger
import java.time.Duration
import java.time.Instant

class PeriodicItems<out T>(
        private val original: ReceiveChannel<RangeTimed<T>>,
        private val cutter: RangeTimedCutter<T>,
        private val merger: RangeTimedMerger<T>,
        private val period: Duration
) {
    fun before(endTime: Instant): ReceiveChannel<RangeTimed<T>> = produce<RangeTimed<T>> {
        var closeTime = endTime
        var combined: RangeTimed<T>? = null

        var remainder: RangeTimed<T>? = null
        val it = original.iterator()
        while (remainder != null || it.hasNext()) {
            val item = remainder ?: it.next()
            val timeRange = closeTime - period..closeTime

            when {
                item.timeRange.endInclusive <= timeRange.start -> {
                    closeTime -= period
                    combined = null
                    remainder = item
                }
                item.timeRange.start < timeRange.endInclusive -> {
                    val leftCandle = cutter.cut(item, Instant.MIN..timeRange.start)
                    val insideCandle = cutter.cut(item, timeRange)

                    combined = merger.mergeNullable(insideCandle, combined)

                    if (combined != null && combined.timeRange == timeRange) {
                        if (combined.timeRange == timeRange) {
                            send(combined)
                            combined = null
                            closeTime -= period
                        }
                    }

                    remainder = leftCandle
                }
            }
        }
    }
}