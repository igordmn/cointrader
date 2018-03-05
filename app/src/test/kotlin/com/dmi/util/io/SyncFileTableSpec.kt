package com.dmi.util.io

import com.dmi.util.collection.Row
import com.dmi.util.collection.Table
import com.dmi.util.test.Spec
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.StringSerializer
import java.util.*


@Serializable
private data class TestConfig(val x: String)

private class TestSource : Table<String, Long> {
    var values: List<Pair<String, Long>> = emptyList()

    override fun rowsAfter(id: String?): ReceiveChannel<Row<String, Long>> {
        val map = TreeMap<String, Long>().apply { putAll(this@TestSource.values) }
        val subMap = if (id != null) {
            map.tailMap(id, false)
        } else {
            map
        }
        return subMap.asSequence().map { Row(it.key, it.value) }.asReceiveChannel()
    }
}

class SyncFileTableSpec : Spec() {
    private val fs = Jimfs.newFileSystem(Configuration.unix())

    init {
        "x" {
            val source = TestSource()
            val dest = testSyncTable(TestConfig("f"), source)
            dest.sync()
            source.values = listOf(
                    "2" to 7L
            )
            dest.sync()
            dest.rowsAfter(null).toList() shouldBe listOf(
                    Row("2", 7L)
            )
        }

        "corrupted" - {

        }
    }

    private suspend fun testSyncTable(
            config: TestConfig,
            source: TestSource
    ) = syncFileTable(
            fs.getPath("/test"),
            TestConfig.serializer(),
            StringSerializer,
            LongFixedSerializer,
            config,
            source
    )
}