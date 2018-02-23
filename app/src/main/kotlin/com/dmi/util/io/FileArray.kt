package com.dmi.util.io

import java.nio.file.Path

class FileArray<T>(private val path: Path): AutoCloseable {
    val size: Int = 0

    suspend fun get(start: Int, end: Int): List<T> {
        require(start in 0 until size)
        require(end in 0 until size)
        TODO()
    }

    suspend fun clear() {

    }

    suspend fun append(items: List<T>) {

    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}