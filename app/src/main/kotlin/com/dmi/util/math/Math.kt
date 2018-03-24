package com.dmi.util.math

infix fun Int.floorDiv(y: Int): Int = Math.floorDiv(this, y)
infix fun Int.ceilDiv(y: Int): Int = -Math.floorDiv(-this, y)
