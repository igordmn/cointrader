package com.dmi.util.io

import com.dmi.util.collection.Row
import com.dmi.util.collection.IdIndex
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
private data class TestEntity(val x: Int)

private typealias TestIndex = IdIndex<TestId>
private typealias TestItem = Row<TestIndex, TestEntity>
//
//private class TestSource(
//        override val config: TestConfig,
//        private val items: List<Indexed<TestId, TestEntity>>,
//        private val reloadCount: Int
//) : SyncSource<TestConfig, TestIndex, TestEntity> {
//    override fun newItems(lastIndex: TestIndex?): ReceiveChannel<TestItem> {
//
//    }
//}

private val testIndexSerializer = IdIndex.serializer(TestId.serializer())

private class TestEntitySerializer : FixedSerializer<TestEntity> {
    override val itemBytes: Int = 3 * 8

    override fun serialize(item: TestEntity, data: ByteBuffer) {
        data.putInt(item.x)
    }

    override fun deserialize(data: ByteBuffer): TestEntity = TestEntity(
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

    private fun array(
            config: TestConfig
    ) = SyncFileTable(
            fs.getPath("/test"),
            TestConfig.serializer(),
            testIndexSerializer,
            TestEntitySerializer()
    )
}