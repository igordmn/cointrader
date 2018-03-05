package com.dmi.util.io

import com.dmi.util.atom.Atom
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

class NullableFileAtom<T : Any>(
        file: Path,
        private val serializer: KSerializer<T>
) : Atom<T?> {
    private val store = AtomicFileDataStore(file)

    suspend override fun invoke(): T? = if (store.exists()) load(serializer, store.read()) else null

    suspend override fun set(value: T?) {
        if (value != null) {
            store.write(dump(serializer, value))
        } else {
            store.remove()
        }
    }
}