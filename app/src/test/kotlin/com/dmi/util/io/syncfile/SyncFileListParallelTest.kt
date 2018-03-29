package com.dmi.util.io.syncfile

import com.dmi.util.concurrent.forEachAsync
import com.dmi.util.io.IntFixedSerializer
import com.dmi.util.io.syncFileList
import com.dmi.util.math.nextInt
import com.dmi.util.restorable.RestorableSource
import com.dmi.util.test.Spec
import com.dmi.util.test.testFileSystem
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.serialization.internal.IntSerializer
import java.nio.file.Path
import java.util.*

class SyncFileListParallelTest : Spec({

    "test" {
        val r = Random()

        val itemCount = 1000..10000
        val sourceCount = 100

        val thread = newFixedThreadPoolContext(4, "test")
        fun List<Int>.asAsyncRestorableSource() = object : RestorableSource<Int, Int> {
            override fun initial(): ReceiveChannel<RestorableSource.Item<Int, Int>> = restored(-1)

            override fun restored(state: Int): ReceiveChannel<RestorableSource.Item<Int, Int>> = produce {
                for (i in state + 1 until this@asAsyncRestorableSource.size) {
                    async(thread) {
                        send(RestorableSource.Item(i, this@asAsyncRestorableSource[i]))
                    }.await()
                }
            }
        }

        val fs = testFileSystem()
        fun sourceList() = List(r.nextInt(itemCount)) { r.nextInt() }
        val sourceLists = List(sourceCount) { sourceList() }
        val sources = sourceLists.map { it.asAsyncRestorableSource() }
        val destinations = List(sources.size) { testSyncList(fs.getPath("/test$it")) }

        destinations.indices.forEachAsync {
            destinations[it].sync(sources[it])
        }

        val destinationLists = destinations.map { it.toList() }
        destinationLists shouldBe sourceLists
    }
})

private suspend fun testSyncList(path: Path) = syncFileList(
        path,
        TestConfig.serializer(),
        IntSerializer,
        IntFixedSerializer,
        TestConfig("f"),
        bufferSize = 500
)