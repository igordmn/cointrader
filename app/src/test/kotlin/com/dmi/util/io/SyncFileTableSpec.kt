package com.dmi.util.io

import com.dmi.util.collection.Row
import com.dmi.util.test.Spec
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer


@Serializable
private data class TestConfig(val x: String)

@Serializable
private data class TestId(val x: String)

@Serializable
private data class TestValue(val x: Int)

private typealias TestRow = Row<TestId, TestValue>
//
//private class TestSource(
//        override val config: TestConfig,
//        private val items: List<Indexed<TestId, TestValue>>,
//        private val reloadCount: Int
//) : SyncSource<TestConfig, TestIndex, TestValue> {
//    override fun newItems(lastIndex: TestIndex?): ReceiveChannel<TestItem> {
//
//    }
//}

private class TestEntitySerializer : FixedSerializer<TestValue> {
    override val itemBytes: Int = 3 * 8

    override fun serialize(item: TestValue, data: ByteBuffer) {
        data.putInt(item.x)
    }

    override fun deserialize(data: ByteBuffer): TestValue = TestValue(
            data.int
    )
}

class SyncFileTableSpec : Spec() {
    private val fs = Jimfs.newFileSystem(Configuration.unix())

    init {
        "x" {
            val array = array(TestConfig("f"))
//            array.syncWith(TestSource(TestConfig("f")))
        }
    }

    private suspend fun array(
            config: TestConfig
    ) = syncFileTable(
            fs.getPath("/test"),
            TestConfig.serializer(),
            TestId.serializer(),
            TestEntitySerializer(),
            config,
            TODO()
    )
}