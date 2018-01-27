package data.cache

import org.mapdb.DBMaker
import org.mapdb.serializer.SerializerString

fun main(args: Array<String>) {
    val f = DBMaker.fileDB("D:\\h.fg").transactionEnable().make()
    val map = f.treeMap("ff", SerializerString(), SerializerString()).createOrOpen()
    map["Gg"] = "fh"
    f.commit()

    println(map["Gg"])

    map.close()
    val map2 = f.treeMap("ff", SerializerString(), SerializerString()).createOrOpen()
    map2["Gjgg"] = "fh"
    f.commit()

    f.close()
}