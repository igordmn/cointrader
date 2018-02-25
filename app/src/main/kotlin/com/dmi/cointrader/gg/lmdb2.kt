package com.dmi.cointrader.gg

import com.google.common.primitives.Ints
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
//    writeLMDB2()
    readLMDB2()
}

fun writeLMDB2() {
    Env.create().setMapSize(5 * 1024 * 1024 * 1024L).setMaxDbs(1).open(File("D:/14/lmdb")).use { env ->
        val db = env.openDbi("moments", DbiFlags.MDB_CREATE)// q, DbiFlags.MDB_INTEGERKEY)
        var i = 0
        val buffer1 = ByteBuffer.allocateDirect(4)//.order(ByteOrder.nativeOrder())
        val buffer2 = ByteBuffer.allocateDirect(50 * 32)//.order(ByteOrder.nativeOrder())
        writeData { list ->
            env.txnWrite().use { tx ->
                db.openCursor(tx).use { cursor ->
                    list.forEach {
                        buffer1.clear()
                        buffer2.clear()
                        buffer1.limit(4)
                        buffer2.limit(it.size)

                        buffer1.putInt(i)
                        buffer2.put(it)
                        buffer1.rewind()
                        buffer2.rewind()
                        cursor.put(buffer1, buffer2)
                        i++
                    }
                }
                tx.commit()
            }
        }
    }
}

fun readLMDB2() {
    Env.create().setMapSize(5 * 1024 * 1024 * 1024L).setMaxDbs(1).open(File("D:/14/lmdb")).use { env ->
        val db = env.openDbi("moments", DbiFlags.MDB_CREATE)

        val buffer1 = ByteBuffer.allocateDirect(4)//.order(ByteOrder.nativeOrder())
        val buffer2 = ByteBuffer.allocateDirect(4)//.order(ByteOrder.nativeOrder())
        readData { start, end ->
            val list = ArrayList<ByteArray>(end - start)
            env.txnRead().use { tx ->
                buffer1.clear()
                buffer2.clear()
                buffer1.limit(4)
                buffer2.limit(4)
                buffer1.put(Ints.toByteArray(start))
                buffer2.put(Ints.toByteArray(end))
                buffer1.rewind()
                buffer2.rewind()
                val cursor = db.iterate(tx, closedOpen(buffer1, buffer2))

                for (g in cursor) {
                    val arrr = ByteArray(50 * 32)
                    g.`val`().get(arrr)
                    list.add(arrr)
                }
            }
            list
        }
    }
}