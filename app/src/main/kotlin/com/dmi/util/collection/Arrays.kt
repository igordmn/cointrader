package com.dmi.util.collection

operator fun <T> Array<T>.set(indices: IntRange, values: List<T>) {
    require(values.size == indices.endInclusive - indices.start + 1)
    var i = indices.start
    values.forEach {
        set(i++, it)
    }
}

operator fun <T> Array<T>.set(indices: IntRange, values: Array<T>) {
    require(values.size == indices.endInclusive - indices.start + 1)
    var i = indices.start
    values.forEach {
        set(i++, it)
    }
}