package com.dmi.util.io

import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * For store data with fixed size in file
 */
class FileDataArray(
        private val file: Path,
        private val itemBytes: Int
) {
    var size: Long = computeSize()
        private set

    fun reduceSize(newSize: Long) {
        require(newSize <= size)
        size = newSize
    }

    suspend fun read(range: LongRange, data: ByteBuffer) {
        require(range.start in 0..size)
        require(range.endInclusive in 0..size)
        require(data.remaining() == (range.endInclusive - range.start).toInt() * itemBytes)
        AsynchronousFileChannel.open(file, StandardOpenOption.READ).use { channel ->
            channel.aRead(data, range.start * itemBytes)
        }
    }

    fun clear() {
        if (Files.exists(file)) {
            Files.delete(file)
        }
        size = 0
    }

    suspend fun append(data: ByteBuffer) {
        require(data.remaining() isMultipliedBy itemBytes)
        Files.createDirectories(file.parent)
        AsynchronousFileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use { channel ->
            channel.aWrite(data, size * itemBytes)
            size = computeSize()
        }
    }

    private fun computeSize(): Long = if (Files.exists(file)) Files.size(file) / itemBytes else 0
}

private infix fun Int.isMultipliedBy(other: Int) = this / other * other == this