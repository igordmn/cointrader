package com.dmi.cointrader.gg

import org.fusesource.lmdbjni.ByteUnit
import org.fusesource.lmdbjni.Constants
import org.fusesource.lmdbjni.Env

fun main(args: Array<String>) {
    readLMDB()
}

fun writeLMDB() {
    Env("D:/14/lmdb").use { env ->
        val mapSizeGrow = 100L
        var mapSize = 100L
        env.setMapSize(5, ByteUnit.GIBIBYTES)
        env.openDatabase("moments", Constants.CREATE or Constants.INTEGERKEY).use { db ->
            var i = 0
            writeData { list ->
                env.createWriteTransaction().use { tx ->
                    db.bufferCursor(tx).use { cursor ->
                        list.forEach {
                            cursor.keyWriteInt(i)
                            cursor.valWriteBytes(it)
                            cursor.put()
                            i++
                        }
                    }
                    tx.commit()
                }
            }
//            if () {
//                mapSize += mapSizeGrow
//                env.setMapSize(mapSize, ByteUnit.MEBIBYTES)
//            }
        }
    }
}

fun readLMDB() {
    Env("D:/14/lmdb").use { env ->
        env.openDatabase("moments").use { db ->
            readData { start, end ->
                val size = end - start
                val list = ArrayList<ByteArray>(size)
                env.createReadTransaction().use { tx ->
                    db.bufferCursor(tx).use { cursor ->
                        cursor.keyWriteInt(start)
                        var i = 0
                        if (cursor.seekKey()) {
                            do {
                                val data = cursor.valBytes()
                                list.add(data)
                                i++
                            } while (i < size && cursor.next())
                        }
                    }
                }
                list
            }
        }
    }
}