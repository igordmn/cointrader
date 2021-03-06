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

object IntFixedSerializer : FixedSerializer<Int> {
    override val itemBytes: Int = 4

    override fun serialize(item: Int, data: ByteBuffer) {
        data.putInt(item)
    }

    override fun deserialize(data: ByteBuffer): Int = data.int
}

object LongFixedSerializer : FixedSerializer<Long> {
    override val itemBytes: Int = 8

    override fun serialize(item: Long, data: ByteBuffer) {
        data.putLong(item)
    }

    override fun deserialize(data: ByteBuffer): Long = data.long
}