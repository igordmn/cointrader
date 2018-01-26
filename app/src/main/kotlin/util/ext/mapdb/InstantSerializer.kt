package util.ext.mapdb

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.serializer.GroupSerializerObjectArray
import java.time.Instant

object InstantSerializer : GroupSerializerObjectArray<Instant>() {
    override fun serialize(out: DataOutput2, value: Instant) {
        out.packLong(value.epochSecond)
        out.packInt(value.nano)
    }

    override fun deserialize(input: DataInput2, available: Int): Instant = Instant.ofEpochSecond(
            input.unpackLong(),
            input.unpackInt().toLong()
    )

    override fun isTrusted(): Boolean = true
}