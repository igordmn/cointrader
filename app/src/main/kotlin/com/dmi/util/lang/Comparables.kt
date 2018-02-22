package com.dmi.util.lang

fun <T : Comparable<T>> min(v1: T, v2: T): T = if (v1 < v2) v1 else v2
fun <T : Comparable<T>> min(vararg values: T): T = values.min()!!
fun <T : Comparable<T>> max(v1: T, v2: T): T = if (v1 > v2) v1 else v2
fun <T : Comparable<T>> max(vararg values: T): T = values.max()!!