package com.dmi.util.collection

interface SuspendArray<out T> {
    val size: Long

    suspend fun get(range: LongRange): List<T>
}