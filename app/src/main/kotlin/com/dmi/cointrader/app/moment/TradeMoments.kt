package com.dmi.cointrader.app.moment

import com.dmi.util.io.AtomicFileDataStore
import com.dmi.util.io.AtomicFileStore
import com.dmi.util.io.FileFixedArray
import com.dmi.util.io.appendToFileName
import com.google.common.hash.Hashing
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.*


interface ComputedFileArray<R> {
    val id: ByteArray
    val size: Long
    suspend fun compute()
    suspend fun get(range: LongRange): List<R>
}

class TransformFileArray<T, R>(
        file: Path,
        serializer: FileFixedArray.Serializer<R>,
        private val other: ComputedFileArray<T>,
        private val transformId: ByteArray,
        private val transform: (Sequence<T>) -> Sequence<R>
) : ComputedFileArray<R> {
    override val id: ByteArray = hash(listOf(transformId, other.id))

    private val idStore = AtomicFileDataStore(file.appendToFileName(".id"))
    private val metaStore = AtomicFileStore(file.appendToFileName(".meta"), Meta.serializer())
    private val fileArray = FileFixedArray(file.appendToFileName(".array"), serializer)

    override suspend fun compute() {
        other.compute()

        if (idStore.exists()) {
            val storedId = idStore.read()
            if (!Arrays.equals(storedId, id)) {
                fileArray.clear()
                metaStore.write(Meta(0, 0))
                idStore.write(id)
            }
        } else {
            metaStore.write(Meta(0, 0))
            idStore.write(id)
        }

//        val meta = met
    }

    override val size: Long = fileArray.size
    override suspend fun get(range: LongRange): List<R> = fileArray.get(range)

    @Serializable
    data class Meta(val otherIndex: Int, val thisIndex: Int)
}

class CombinedFileArray<T>(
        file: Path,
        serializer: FileFixedArray.Serializer<T>,
        private val params: List<ComputedFileArray<T>>
) : ComputedFileArray<List<T>> {
    override val id: ByteArray = hash(params.map(ComputedFileArray<T>::id))

    private val idStore = AtomicFileDataStore(file.appendToFileName(".id"))
    private val fileArray = FileFixedArray(file.appendToFileName(".array"), ListSerializer(params.size, serializer))

    init {
        require(params.isNotEmpty())
    }

    override suspend fun compute() {
        val windowSize = 1000L

        params.forEach {
            it.compute()
        }

        if (idStore.exists()) {
            val storedId = idStore.read()
            if (!Arrays.equals(storedId, id)) {
                fileArray.clear()
                idStore.write(id)
            }
        } else {
            idStore.write(id)
        }

        val minSize = params.minBy(ComputedFileArray<T>::size)!!.size
        val storedSize = fileArray.size

        (storedSize until minSize).rangeChunked(windowSize).forEach { range ->
            val itemToParams = params.map { it.get(range) }.transpose()
            fileArray.append(itemToParams)
        }
    }

    override val size: Long = fileArray.size
    override suspend fun get(range: LongRange): List<List<T>> = fileArray.get(range)
}

private fun hash(arrays: List<ByteArray>): ByteArray {
    val outputStream = ByteArrayOutputStream()
    arrays.forEach {
        outputStream.write(it)
    }
    return Hashing.murmur3_128().hashBytes(outputStream.toByteArray()).asBytes()
}

private fun LongRange.rangeChunked(size: Long): List<LongRange> {
    val ranges = ArrayList<LongRange>()
    for (st in start until endInclusive step size) {
        val nd = Math.min(endInclusive, st + size)
        ranges.add(LongRange(st, nd))
    }
    return ranges
}

private fun <T> List<List<T>>.transpose(): List<List<T>> {
    val newSize = first().size
    return (0 until newSize).map { i -> this.map { it[i] } }
}

private class ListSerializer<T>(
        private val size: Int,
        private val original: FileFixedArray.Serializer<T>
) : FileFixedArray.Serializer<List<T>> {
    override val itemBytes: Int = original.itemBytes * size

    override fun serialize(item: List<T>, data: ByteBuffer) = item.forEach {
        original.serialize(it, data)
    }

    override fun deserialize(data: ByteBuffer): List<T> = (1..size).map {
        original.deserialize(data)
    }
}