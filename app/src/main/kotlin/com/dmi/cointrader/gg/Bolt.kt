package com.dmi.cointrader.gg

import com.google.common.primitives.Ints
import jetbrains.exodus.ArrayByteIterable
import jetbrains.exodus.ByteIterable
import jetbrains.exodus.env.EnvironmentConfig
import jetbrains.exodus.env.Environments
import jetbrains.exodus.env.StoreConfig
import org.lmdbjava.DbiFlags
import org.lmdbjava.Env
import org.lmdbjava.KeyRange
import org.lmdbjava.KeyRange.all
import org.lmdbjava.KeyRange.closedOpen
import org.lmdbjava.KeyRangeType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main(args: Array<String>) {
    writeBolt()
    readBolt()
}

fun writeBolt() {
    Environments.newInstance("D:/14/xodus",
            EnvironmentConfig()
                    .setEnvGatherStatistics(false)
                    .setEnvGatherStatistics(false)
                    .setTreeMaxPageSize(256)
    ).use { env ->
        var i = 0
        writeData { list ->
            val tx = env.beginTransaction()

            val store = env.openStore("moments", StoreConfig.WITHOUT_DUPLICATES, tx)
            list.forEach {
                store.putRight(tx, ArrayByteIterable(Ints.toByteArray(i)), ArrayByteIterable(it))
                i++
            }
            tx.commit()
        }
    }
}

fun readBolt() {
    Environments.newInstance("D:/14/xodus",
            EnvironmentConfig()
                    .setEnvGatherStatistics(false)
                    .setEnvGatherStatistics(false)
                    .setTreeMaxPageSize(256)
    ).use { env ->
        val tx = env.beginReadonlyTransaction()
        val store = env.openStore("moments", StoreConfig.WITHOUT_DUPLICATES, tx)
        readData { start, end ->
            val list = ArrayList<ByteArray>(end - start)
            store.openCursor(tx).use { cursor ->
                cursor.getSearchKey(ArrayByteIterable(Ints.toByteArray(start)))
                list.add(cursor.value.toArray())

                while (list.size < end - start && cursor.next) {
                    list.add(cursor.value.toArray())
                }
            }
            list
        }
    }
}

private fun ByteIterable.toArray(): ByteArray {
    val arr = ByteArray(length)
    val it = iterator()
    var i = 0
    while (it.hasNext()) {
        arr[i++] = it.next()
    }
    return arr
}