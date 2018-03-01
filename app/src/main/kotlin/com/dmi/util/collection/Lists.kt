package com.dmi.util.collection

fun <T> List<List<T>>.zip(): List<List<T>> {
    val newSize = minBy { it.size }!!.size
    return (0 until newSize).map { i -> this.map { it[i] } }
}