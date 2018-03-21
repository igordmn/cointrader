package com.dmi.util.collection

fun <T> Array<T>.set(indices: IntRange, values: List<T>) {
    require(values.size == indices.endInclusive - indices.start + 1)
    var i = indices.start
    values.forEach {
        set(i++, it)
    }
}