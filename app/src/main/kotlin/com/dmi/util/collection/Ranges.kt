package com.dmi.util.collection

import kotlin.coroutines.experimental.buildSequence

fun LongRange.rangeChunked(size: Long): Sequence<LongRange> = buildSequence {
    for (st in this@rangeChunked step size) {
        val nd = Math.min(endInclusive, st + size - 1)
        yield(LongRange(st, nd))
    }
}