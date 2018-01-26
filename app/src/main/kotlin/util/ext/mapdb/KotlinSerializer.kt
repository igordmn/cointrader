package util.ext.mapdb

import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.CBOR.Companion.dump
import kotlinx.serialization.cbor.CBOR.Companion.load
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.serializer.GroupSerializerObjectArray

inline fun <reified T : Any> kotlinSerializer(serializer: KSerializer<T>) = object : GroupSerializerObjectArray<T>() {
    override fun serialize(out: DataOutput2, value: T) {
        out.write(dump(serializer, value))
    }

    override fun deserialize(input: DataInput2, available: Int): T {
        val stream = DataInput2.DataInputToStream(input)
        return load(serializer, stream.readBytes())
    }
}