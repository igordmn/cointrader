package com.dmi.util.io

import com.dmi.util.test.Spec
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import java.nio.file.FileSystem
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
    "simple" - {
        "empty" {
            val fs = Jimfs.newFileSystem(Configuration.unix())
            val source = TestSource()
            val dest = testSyncList(fs, TestConfig("f"), source)
            dest.toList() shouldBe emptyList<TestDestRow>()
            dest.sync()
            dest.toList() shouldBe emptyList<TestDestRow>()
        }
        
        "sync new values" {
            val fs = Jimfs.newFileSystem(Configuration.unix())
            val source = TestSource()
            val dest = testSyncList(fs, TestConfig("f"), source)

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
                    100L to 991L,
                    110L to 991L,
                    120L to 991L
            )
            dest.sync()
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L, 991L, 991L, 991L)
        }
    }

    "restore" {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val source = TestSource()
        val dest1 = testSyncList(fs, TestConfig("f"), source)

        source.values = listOf(
                20L to 7L,
                40L to 8L,
                88L to 88L,
                99L to 99L
        )
        dest1.sync()
        dest1.toList() shouldBe listOf(7L, 8L, 88L, 99L)

        val dest2 = testSyncList(fs, TestConfig("f"), source)
        dest2.toList() shouldBe listOf(7L, 8L, 88L, 99L)
        dest2.sync()
        dest2.toList() shouldBe listOf(7L, 8L, 88L, 99L)

        val dest3 = testSyncList(fs, TestConfig("ff"), source)
        dest3.toList() shouldBe emptyList<Long>()
        dest3.sync()
        dest3.toList() shouldBe listOf(7L, 8L, 88L, 99L)
    }

    "reload last" {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val source = TestSource()
        val dest = testSyncList(fs, TestConfig("f"), source, reloadCount = 3)

        source.values = listOf(
                20L to 7L
        )
        dest.sync()
        dest.toList() shouldBe listOf(7L)

        source.values = listOf(
                20L to 8L
        )
        dest.sync()
        dest.toList() shouldBe listOf(8L)

        source.values = listOf(
                20L to 9L,
                30L to 10L
        )
        dest.sync()
        dest.toList() shouldBe listOf(9L, 10L)

        source.values = listOf(
                20L to 10L
        )
        dest.sync()
        dest.toList() shouldBe listOf(10L)

        source.values = listOf(
                20L to 9L,
                30L to 20L,
                40L to 30L,
                50L to 40L
        )
        dest.sync()
        dest.toList() shouldBe listOf(9L, 20L, 30L, 40L)

        source.values = listOf(
                20L to 8L,
                30L to 10L,
                40L to 20L,
                50L to 30L
        )
        dest.sync()
        dest.toList() shouldBe listOf(9L, 10L, 20L, 30L)
    }

    "corrupted" - {

    }
})

private suspend fun testSyncList(
        fs: FileSystem,
        config: TestConfig,
        source: TestSource,
        reloadCount: Int = 0
) = syncFileList(
        fs.getPath("/test"),
        TestConfig.serializer(),
        LongSerializer,
        LongFixedSerializer,
        config,
        source,
        bufferSize = 2,
        reloadCount = reloadCount
)