package com.dmi.util.io

import kotlinx.coroutines.experimental.nio.aRead
import kotlinx.coroutines.experimental.nio.aWrite
import kotlinx.serialization.KSerialLoader
import kotlinx.serialization.KSerialSaver
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.CBOR.Companion.dump
import kotlinx.serialization.cbor.CBOR.Companion.load
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class AtomicFileStore<T : Any>(
        file: Path,
        private val serializer: KSerializer<T>
) {
    private val dataStore = AtomicFileDataStore(file)

    fun exists() = dataStore.exists()
    fun remove() = dataStore.remove()

    suspend fun write(obj: T) = dataStore.write(dump(serializer, obj))
    suspend fun read(): T = load(serializer, dataStore.read())
    suspend fun readOrNull(): T? = if (exists()) read() else null
}