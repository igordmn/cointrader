package com.dmi.util.io.syncfile

import com.dmi.util.concurrent.forEachAsync
import com.dmi.util.io.IntFixedSerializer
import com.dmi.util.io.syncFileList
import com.dmi.util.math.nextInt
import com.dmi.util.restorable.asRestorableSource
import com.dmi.util.test.Spec
import com.dmi.util.test.testFileSystem
import io.kotlintest.matchers.shouldBe
import kotlinx.serialization.internal.IntSerializer
import java.nio.file.Path
import java.util.*

class SyncFileListParallelTest : Spec({
    "test" {
        val itemCount = 1000..10000
        val sourceCount = 100

        val r = Random()
        val fs = testFileSystem()
        fun sourceList() = List(r.nextInt(itemCount)) { r.nextInt() }
        val sourceLists = List(sourceCount) { sourceList() }
        val sources = sourceLists.map { it.asRestorableSource() }
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
        bufferSize = 30
)