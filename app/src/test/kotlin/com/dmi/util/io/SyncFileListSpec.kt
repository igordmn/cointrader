package com.dmi.util.io

import com.dmi.util.restorable.RestorableSource
import com.dmi.util.test.Spec
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.LongSerializer
import java.nio.file.FileSystem
import java.nio.file.Files
import java.util.*


@Serializable
private data class TestConfig(val x: String)

private typealias TestSourceRow = RestorableSource.Item<Long, Long>
private typealias TestDestRow = RestorableSource.Item<Long, Long>

private class TestSource(private val values: List<Pair<Long, Long>>) : RestorableSource<Long, Long> {
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

class SyncFileListSpec : Spec({
    "simple" - {
        "empty" {
            val fs = Jimfs.newFileSystem(Configuration.unix())
            val dest = testSyncList(fs, TestConfig("f"))
            dest.toList() shouldBe emptyList<TestDestRow>()
            dest.sync(TestSource(emptyList()))
            dest.toList() shouldBe emptyList<TestDestRow>()
        }

        "sync new values" {
            val fs = Jimfs.newFileSystem(Configuration.unix())
            val dest = testSyncList(fs, TestConfig("f"))

            dest.sync(TestSource(listOf(
                    2L to 7L
            )))
            dest.toList() shouldBe listOf(7L)

            dest.sync(TestSource(listOf(
                    2L to 7L
            )))
            dest.toList() shouldBe listOf(7L)
            
            dest.sync(TestSource(listOf(
                    2L to 73L,
                    4L to 8L
            )))
            dest.toList() shouldBe listOf(7L, 8L)
            
            dest.sync(TestSource(listOf(
                    2L to 7L,
                    4L to 8L,
                    88L to 88L,
                    99L to 99L
            )))
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L)

            dest.sync(TestSource(listOf(
                    20L to 7L,
                    40L to 8L,
                    88L to 88L,
                    99L to 99L
            )))
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L)

            dest.sync(TestSource(listOf(
                    2L to 70L,
                    4L to 80L,
                    88L to 880L,
                    99L to 990L
            )))
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L)

            dest.sync(TestSource(listOf(
                    2L to 70L,
                    4L to 80L,
                    88L to 880L,
                    99L to 990L,
                    100L to 991L,
                    110L to 991L,
                    120L to 991L
            )))
            dest.toList() shouldBe listOf(7L, 8L, 88L, 99L, 991L, 991L, 991L)
        }
    }

    "restore" {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val dest1 = testSyncList(fs, TestConfig("f"))

        dest1.sync(TestSource(listOf(
                20L to 7L,
                40L to 8L,
                88L to 88L,
                99L to 99L
        )))
        dest1.toList() shouldBe listOf(7L, 8L, 88L, 99L)

        val dest2 = testSyncList(fs, TestConfig("f"))
        dest2.toList() shouldBe listOf(7L, 8L, 88L, 99L)
        dest2.sync(TestSource(listOf(
                20L to 7L,
                40L to 8L,
                88L to 88L,
                99L to 99L
        )))
        dest2.toList() shouldBe listOf(7L, 8L, 88L, 99L)

        val dest3 = testSyncList(fs, TestConfig("ff"))
        dest3.toList() shouldBe emptyList<Long>()
        dest3.sync(TestSource(listOf(
                20L to 7L,
                40L to 8L,
                88L to 88L,
                99L to 99L
        )))
        dest3.toList() shouldBe listOf(7L, 8L, 88L, 99L)
    }

    "reload last" {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val dest = testSyncList(fs, TestConfig("f"), reloadCount = 3)

        dest.sync(TestSource(listOf(
                20L to 7L
        )))
        dest.toList() shouldBe listOf(7L)

        dest.sync(TestSource(listOf(
                20L to 8L
        )))
        dest.toList() shouldBe listOf(8L)

        dest.sync(TestSource(listOf(
                20L to 9L,
                30L to 10L
        )))
        dest.toList() shouldBe listOf(9L, 10L)

        dest.sync(TestSource(listOf(
                20L to 10L
        )))
        dest.toList() shouldBe listOf(10L)

        dest.sync(TestSource(listOf(
                20L to 9L,
                30L to 20L,
                40L to 30L,
                50L to 40L
        )))
        dest.toList() shouldBe listOf(9L, 20L, 30L, 40L)

        dest.sync(TestSource(listOf(
                20L to 8L,
                30L to 10L,
                40L to 20L,
                50L to 30L
        )))
        dest.toList() shouldBe listOf(9L, 10L, 20L, 30L)
    }

    "corrupted" - {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        val dest1 = testSyncList(fs, TestConfig("f"))

        val file = fs.getPath("/test")
        val configFile = file.appendToFileName(".config")
        val lastInfoFile = file.appendToFileName(".lastInfo")

        dest1.sync(TestSource(listOf(
                20L to 8L,
                30L to 10L,
                40L to 20L,
                50L to 30L
        )))

        "config file not created" {
            Files.delete(configFile)

            val dest2 = testSyncList(fs, TestConfig("f"))
            dest2.toList() shouldBe emptyList<Long>()
            dest2.sync(TestSource(listOf(
                    20L to 9L,
                    30L to 10L,
                    40L to 20L,
                    50L to 30L
            )))
            dest2.toList() shouldBe listOf(9L, 10L, 20L, 30L)
        }

        "last info file not created" {
            Files.delete(lastInfoFile)

            val dest2 = testSyncList(fs, TestConfig("f"))
            dest2.toList() shouldBe listOf(8L, 10L, 20L, 30L)
            dest2.sync(TestSource(listOf(
                    20L to 9L,
                    30L to 10L,
                    40L to 20L,
                    50L to 30L
            )))
            dest2.toList() shouldBe listOf(9L, 10L, 20L, 30L)
        }
    }
})

private suspend fun testSyncList(
        fs: FileSystem,
        config: TestConfig,
        reloadCount: Int = 0
) = syncFileList(
        fs.getPath("/test"),
        TestConfig.serializer(),
        LongSerializer,
        LongFixedSerializer,
        config,
        bufferSize = 2,
        reloadCount = reloadCount
)