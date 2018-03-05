package com.dmi.util.io

import java.nio.ByteBuffer
import java.nio.file.Path

class FileArray<T>(
        file: Path,
        private val serializer: FixedSerializer<T>
) {
    private val dataArray = FileDataArray(file, serializer.itemBytes)

    val size: Long get() = dataArray.size
    fun reduceSize(newSize: Long) = dataArray.reduceSize(newSize)
    fun clear() = dataArray.clear()

    suspend fun get(range: LongRange): List<T> {
        require(range.start in 0 until size)
        require(range.endInclusive in 0 until size)

        val end = range.endInclusive + 1
        val size = (end - range.start).toInt()
        return readList(size) {
            dataArray.read(range, it)
        }
    }

    suspend fun append(items: List<T>) {
        dataArray.append(toBuffer(items))
    }

    private suspend fun readList(size: Int, read: suspend (ByteBuffer) -> Unit): List<T> {
        val buffer = ByteBuffer.allocate(size * serializer.itemBytes)
        read(buffer)
        buffer.rewind()

        val items = ArrayList<T>(size)
        (1..size).forEach {
            items.add(serializer.deserialize(buffer))
        }
        return items
    }

    private fun toBuffer(items: List<T>): ByteBuffer {
        val buffer = ByteBuffer.allocate(items.size * serializer.itemBytes)
        items.forEach {
            serializer.serialize(it, buffer)
        }
        buffer.rewind()
        return buffer
    }
}