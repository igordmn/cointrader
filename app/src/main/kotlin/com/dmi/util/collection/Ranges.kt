package com.dmi.util.collection

fun LongRange.rangeChunked(size: Long): List<LongRange> {
    val ranges = ArrayList<LongRange>()
    for (st in start until endInclusive step size) {
        val nd = Math.min(endInclusive, st + size)
        ranges.add(LongRange(st, nd))
    }
    return ranges
}

interface OpenRightRange<T: Comparable<T>> {
    val start: T
    val end: T

    operator fun contains(value: T): Boolean = value >= start && value < end

    fun isEmpty(): Boolean = start >= end
}

fun <T: Comparable<T>> openRight(range: ClosedRange<T>) = object: OpenRightRange<T> {
    override val start: T = range.start
    override val end: T = range.endInclusive
}

typealias LongOpenRightRange = OpenRightRange<Long>