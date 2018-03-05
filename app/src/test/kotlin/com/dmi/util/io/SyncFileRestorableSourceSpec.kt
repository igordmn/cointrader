package com.dmi.util.io

import com.dmi.util.test.Spec
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import kotlinx.serialization.internal.StringSerializer
import java.util.*


@Serializable
private data class TestConfig(val x: String)

private typealias TestSourceRow = RestorableSource.Item<Long, Long>
private typealias TestDestRow = RestorableSource.Item<Long, Long>

private class TestSource : RestorableSource<Long, Long> {
    var values: List<Pair<Long, Long>> = emptyList()

    override fun restore(state: Long?): ReceiveChannel<TestSourceRow> {
        val map = TreeMap<Long, Long>().apply { putAll(this@TestSource.values) }
        val subMap = if (state != null) {
            map.tailMap(state, false)
        } else {
            map
        }
        return subMap.asSequence().map { TestSourceRow(it.key, it.value) }.asReceiveChannel()
    }
}

class SyncFileRestorableSourceSpec : Spec({
    val source = TestSource()
    val dest = testSyncTable(TestConfig("f"), source)

    "simple" - {
        "empty" {
            dest.toList() shouldBe emptyList<TestDestRow>()
            dest.sync()
            dest.toList() shouldBe emptyList<TestDestRow>()
        }
        
        "sync new values" {
            source.values = listOf(
                    2L to 7L
            )
            dest.toList() shouldBe emptyList<TestDestRow>()

            dest.sync()
            dest.toList() shouldBe listOf(7L)

            dest.sync()
            dest.toList() shouldBe listOf(7L)

            source.values = listOf(
                    2L to 73L,
                    4L to 8L
            )
            dest.sync()
            dest.toList() shouldBe listOf(7L, 8L)

            source.values = listOf(
                    2L to 7L,
                    4L to 8L,
                    88L to 88L,
                    99L to 99L
            )
            dest.sync()
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L)

            source.values = listOf(
                    20L to 7L,
                    40L to 8L,
                    88L to 88L,
                    99L to 99L
            )
            dest.sync()
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L)

            source.values = listOf(
                    2L to 70L,
                    4L to 80L,
                    88L to 880L,
                    99L to 990L
            )
            dest.sync()
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L)

            source.values = listOf(
                    2L to 70L,
                    4L to 80L,
                    88L to 880L,
                    99L to 990L,
                    100L to 991L
            )
            dest.sync()
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L, 991L)
        }
    }

    "reload" - {

    }

    "small buffer size" - {

    }

    "corrupted" - {

    }
})

private suspend fun testSyncTable(
        config: TestConfig,
        source: TestSource
) = syncFileList(
        Jimfs.newFileSystem(Configuration.unix()).getPath("/test"),
        TestConfig.serializer(),
        LongSerializer,
        LongFixedSerializer,
        config,
        source
)