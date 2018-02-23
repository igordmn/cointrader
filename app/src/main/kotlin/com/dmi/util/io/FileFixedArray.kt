package com.dmi.util.io

import java.nio.file.Path

class FileFixedArray<T>(private val dataArray: FileFixedDataArray) {
    val size: Long get() = dataArray.size

    suspend fun get(start: Int, end: Int): List<T> {
        require(start in 0 until size)
        require(end in 0 until size)
        TODO()
    }

    suspend fun clear() {

    }

    suspend fun append(items: List<T>) {

    }
}