package com.dmi.util.collection

import kotlin.coroutines.experimental.buildSequence

fun LongRange.rangeChunked(size: Long): Sequence<LongRange> = buildSequence {
    for (st in this@rangeChunked step size) {
        val nd = Math.min(endInclusive, st + size - 1)
        yield(LongRange(st, nd))
    }
}

fun LongRange.toInt() = start.toInt()..endInclusive.toInt()

fun <T : Comparable<T>, R : Comparable<R>> ClosedRange<T>.map(transform: (T) -> R): ClosedRange<R> = transform(start)..transform(endInclusive)