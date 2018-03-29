package com.dmi.util.io.syncfile

import com.dmi.util.restorable.RestorableSource
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TestConfig(val x: String)

typealias TestSourceRow = RestorableSource.Item<Long, Long>
typealias TestDestRow = RestorableSource.Item<Long, Long>

class TestSource(private val values: List<Pair<Long, Long>>) : RestorableSource<Long, Long> {
    override fun initial() = channel(null)
    override fun restored(state: Long) = channel(state)

    private fun channel(state: Long?): ReceiveChannel<TestSourceRow> {
        val map = TreeMap<Long, Long>().apply { putAll(this@TestSource.values) }
        val subMap = if (state != null) {
            map.tailMap(state, false)
        } else {
            map
        }
        return subMap.asSequence().map { TestSourceRow(it.key, it.value) }.asReceiveChannel()
    }
}