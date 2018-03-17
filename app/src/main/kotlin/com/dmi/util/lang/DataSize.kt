package com.dmi.util.lang

fun Long.bytes() = DataSize(this)
fun Double.kilobytes() = DataSize((this * 1024).toLong())
fun Double.megabytes() = DataSize((this * 1024 * 1024).toLong())
fun Double.gigabytes() = DataSize((this * 1024 * 1024 * 1024).toLong())
fun Double.terabytes() = DataSize((this * 1024 * 1024 * 1024 * 1024).toLong())

class DataSize(val bytes: Long) {
    val kilobytes: Double get() = bytes / 1024.0
    val megabytes: Double get() = bytes / 1024.0 / 1024.0
    val gigabytes: Double get() = bytes / 1024.0 / 1024.0 / 1024.0
    val terabytes: Double get() = bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0
}