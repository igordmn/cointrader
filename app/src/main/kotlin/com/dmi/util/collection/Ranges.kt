package com.dmi.util.collection

import com.dmi.util.math.floorDiv
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.min

fun IntRange.chunked(size: Int): Sequence<IntRange> = buildSequence {
    for (st in this@chunked step size) {
        val nd = min(endInclusive, st + size - 1)
        yield(IntRange(st, nd))
    }
}

fun LongRange.chunked(size: Long): Sequence<LongRange> = buildSequence {
    for (st in this@chunked step size) {
        val nd = min(endInclusive, st + size - 1)
        yield(LongRange(st, nd))
    }
}

fun <T : Comparable<T>, R : Comparable<R>> ClosedRange<T>.rangeMap(
        transform: (T) -> R
): ClosedRange<R> = transform(start)..transform(endInclusive)

fun IntRange.toLong() = start.toLong()..endInclusive.toLong()

fun IntProgression.size() = 1 + ((last - first) floorDiv step)

fun IntProgression.slice(indices: IntRange): IntProgression {
    require(indices in this.indices())
    return first + step * indices.first..first + step * indices.last step step
}

fun IntProgression.indices() = 0 until size()

operator fun <T : Comparable<T>> ClosedRange<T>.contains(another: ClosedRange<T>): Boolean {
    return start <= another.start && another.endInclusive <= endInclusive
}

fun IntRange.coerceIn(other: IntRange): IntRange = start.coerceIn(other)..endInclusive.coerceIn(other)