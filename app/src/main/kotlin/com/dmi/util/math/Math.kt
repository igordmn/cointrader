package com.dmi.util.math

infix fun Int.floorDiv(y: Int): Int = Math.floorDiv(this, y)
infix fun Int.ceilDiv(y: Int): Int = -Math.floorDiv(-this, y)
infix fun Long.floorDiv(y: Long): Long = Math.floorDiv(this, y)
infix fun Long.ceilDiv(y: Long): Long = -Math.floorDiv(-this, y)