package com.dmi.util.io

import java.nio.ByteBuffer
import java.nio.file.Path

class FileFixedArray<T>(
        file: Path,
        private val serializer: Serializer<T>
) {
    private val dataArray = FileFixedDataArray(file, serializer.itemBytes)

    val size: Long get() = dataArray.size

    fun reduceSize(newSize: Long) = dataArray.reduceSize(newSize)

    suspend fun get(range: LongRange): List<T> {
        require(range.start in 0..size)
        require(range.endInclusive in 0..size)

        val size = (range.endInclusive - range.start).toInt()
        val buffer = ByteBuffer.allocate(size * serializer.itemBytes)
        dataArray.read(range, buffer)
        buffer.rewind()

        val items = ArrayList<T>(size)
        (1..size).forEach {
            items.add(serializer.deserialize(buffer))
        }
        return items
    }

    fun clear() = dataArray.clear()

    suspend fun append(items: List<T>) {
        val buffer = ByteBuffer.allocate(items.size * serializer.itemBytes)
        items.forEach {
            serializer.serialize(it, buffer)
        }
        dataArray.append(buffer)
    }

    interface Serializer<T> {
        val itemBytes: Int

        fun serialize(item: T, data: ByteBuffer)
        fun deserialize(data: ByteBuffer): T
    }
}