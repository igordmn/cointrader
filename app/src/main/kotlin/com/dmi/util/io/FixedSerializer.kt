package com.dmi.util.io

import java.nio.ByteBuffer

interface FixedSerializer<T> {
    val itemBytes: Int

    fun serialize(item: T, data: ByteBuffer)
    fun deserialize(data: ByteBuffer): T
}

class FixedListSerializer<T>(
        private val size: Int,
        private val original: FixedSerializer<T>
) : FixedSerializer<List<T>> {
    override val itemBytes: Int = original.itemBytes * size

    override fun serialize(item: List<T>, data: ByteBuffer) = item.forEach {
        original.serialize(it, data)
    }

    override fun deserialize(data: ByteBuffer): List<T> = (1..size).map {
        original.deserialize(data)
    }
}