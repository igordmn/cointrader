package util.ext.mapdb

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.serializer.GroupSerializerObjectArray
import org.mapdb.serializer.SerializerJava
import java.io.Serializable

class SerializableSerializer<T : Serializable>  : GroupSerializerObjectArray<T>() {
    private val serializerJava = SerializerJava()

    override fun serialize(out: DataOutput2, value: T) = serializerJava.serialize(out, value)

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(input: DataInput2, available: Int): T = serializerJava.deserialize(input, available) as T
}