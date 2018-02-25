package com.dmi.util.io

import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class AtomicFileStore(private val file: Path) {
    private val tempFile = file.appendToFileName(".tmp")

    suspend fun write(data: ByteArray) {
        AsynchronousFileChannel.open(
                tempFile,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        ).use { channel ->
            val buffer = ByteBuffer.allocate(data.size)
            buffer.put(data)
            buffer.rewind()
            channel.aWrite(buffer, 0)
        }
        if (Files.exists(file)) {
            Files.delete(file)
        }
        Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE)
    }

    fun exists(): Boolean = Files.exists(file) || Files.exists(tempFile)

    fun remove() {
        if (Files.exists(file)) {
            Files.delete(file)
        }
        if (Files.exists(tempFile)) {
            Files.delete(tempFile)
        }
    }

    suspend fun read(): ByteArray {
        require(exists())

        restoreIfCorrupted()

        AsynchronousFileChannel.open(file, StandardOpenOption.READ).use { channel ->
            val size = channel.size().toInt()
            val buffer = ByteBuffer.allocate(size)
            channel.aRead(buffer, 0)
            buffer.rewind()
            val result = ByteArray(size)
            buffer.get(result)
            return result
        }
    }

    private fun restoreIfCorrupted() {
        if (!Files.exists(file)) {
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE)
        } else if (Files.exists(tempFile)) {
            Files.delete(tempFile)
        }
    }
}