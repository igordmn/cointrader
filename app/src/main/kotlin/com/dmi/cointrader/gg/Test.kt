package com.dmi.cointrader.gg

import java.util.*
import kotlin.system.measureNanoTime

fun writeData(write: (List<ByteArray>) -> Unit) {
    println(356 * 24 * 60)
    (0..356).forEach {
        println("write $it " + measureNanoTime {
            write(List(24 * 60) { ByteArray(50 * 32) { it.toByte() } })
        } / 1e6)
    }
}

fun readData(read: (start: Int, end: Int) -> List<ByteArray>) {
    val r = Random()
    (1..5).forEach {
        println("read " + measureNanoTime {
            (1..1000).forEach {
                val start = r.nextInt(100 * 24 * 60)
                val end = start + 1500
                val data = read(start, end)
                require(data.size == 1500)
            }
        } / 1e6)
    }
}