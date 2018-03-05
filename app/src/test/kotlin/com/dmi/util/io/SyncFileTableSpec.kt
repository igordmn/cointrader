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

private typealias TestSourceRow = Row<String, Long>
private typealias TestDestRow = Row<Long, Long>

private class TestSource : Table<String, Long> {
    var values: List<Pair<String, Long>> = emptyList()

    override fun rowsAfter(id: String?): ReceiveChannel<TestSourceRow> {
        val map = TreeMap<String, Long>().apply { putAll(this@TestSource.values) }
        val subMap = if (id != null) {
            map.tailMap(id, false)
        } else {
            map
        }
        return subMap.asSequence().map { Row(it.key, it.value) }.asReceiveChannel()
    }
}

class SyncFileTableSpec : Spec({
    val source = TestSource()
    val dest = testSyncTable(TestConfig("f"), source)

    "simple" - {
        "initial" {
            dest.rowsAfter(null).toList().map(TestDestRow::toPair) shouldBe emptyList<TestDestRow>()
            dest.rowsAfter(1).toList() shouldBe emptyList<TestDestRow>()
        }
        
        "single item" {
            source.values = listOf(
                    "2" to 7L
            )

            dest.rowsAfter(null).toList() shouldBe emptyList<TestDestRow>()
            dest.rowsAfter(0).toList() shouldBe emptyList<TestDestRow>()
            dest.rowsAfter(1).toList() shouldBe emptyList<TestDestRow>()

            dest.sync()
            
            dest.rowsAfter(null).toList() shouldBe listOf(0 to 7L)
            dest.rowsAfter(0).toList() shouldBe emptyList<TestDestRow>()
            dest.rowsAfter(1).toList() shouldBe emptyList<TestDestRow>()

            dest.sync()

            dest.rowsAfter(null).toList() shouldBe listOf(0 to 7L)
            dest.rowsAfter(0).toList() shouldBe emptyList<TestDestRow>()
            dest.rowsAfter(1).toList() shouldBe emptyList<TestDestRow>()
        }
    }

    "reload" - {

    }

    "corrupted" - {

    }
})

private suspend fun testSyncTable(
        config: TestConfig,
        source: TestSource
) = syncFileTable(
        Jimfs.newFileSystem(Configuration.unix()).getPath("/test"),
        TestConfig.serializer(),
        StringSerializer,
        LongFixedSerializer,
        config,
        source
)