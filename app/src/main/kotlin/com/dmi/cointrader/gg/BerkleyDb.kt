package com.dmi.cointrader.gg

import com.google.common.primitives.Ints
import com.sleepycat.persist.model.Entity
import com.sleepycat.persist.model.PrimaryKey
import com.sleepycat.persist.PrimaryIndex
import java.io.File
import com.sleepycat.persist.EntityStore
import com.sleepycat.persist.StoreConfig
import com.sleepycat.persist.model.Persistent
import java.time.Duration
import java.time.Instant
import com.sleepycat.compat.DbCompat.openDatabase
import com.sleepycat.je.*
import com.sleepycat.je.OperationStatus
import javafx.scene.Cursor.cursor
import com.sleepycat.je.DatabaseEntry




fun main(args: Array<String>) {
    readBDB()
}

private fun writeBDB() {
    val envConfig = EnvironmentConfig()
    envConfig.allowCreate = true
    envConfig.transactional = true
    envConfig.cacheSize = 10 * 1024 * 1024
    Environment(File("D:/14/bdb"), envConfig).use { env ->
        val storeConfig = StoreConfig()
        storeConfig.allowCreate = true
        storeConfig.transactional = true

        val txConf = TransactionConfig()
        txConf.readCommitted = true

        val dbConfig = DatabaseConfig()
        dbConfig.allowCreate = true
        dbConfig.deferredWrite = true
        env.openDatabase(null, "sampleDatabase", dbConfig).use { db ->
            var i = 0
            writeData { list ->
                db.openCursor(null, CursorConfig.DEFAULT).use { cursor ->
                    list.forEach {
                        cursor.put(DatabaseEntry(Ints.toByteArray(i)), DatabaseEntry(it))
                        i++
                    }
                }
                db.sync()
            }
        }
    }
}

private fun readBDB() {
    val envConfig = EnvironmentConfig()
    envConfig.allowCreate = true
    envConfig.transactional = true
    envConfig.cacheSize = 10 * 1024 * 1024
    Environment(File("D:/14/bdb"), envConfig).use { env ->
        val storeConfig = StoreConfig()
        storeConfig.allowCreate = true
        storeConfig.transactional = true

        val txConf = TransactionConfig()
        txConf.readCommitted = true

        val dbConfig = DatabaseConfig()
        dbConfig.allowCreate = true
        dbConfig.deferredWrite = true
        env.openDatabase(null, "sampleDatabase", dbConfig).use { db ->
            readData({ start, end ->
                val list = ArrayList<ByteArray>()

                db.openCursor(null, CursorConfig.DEFAULT).use { cursor ->
                    val foundKey = DatabaseEntry()
                    val foundData = DatabaseEntry()
                    cursor.getSearchKey(DatabaseEntry(Ints.toByteArray(start)), foundData, LockMode.DEFAULT)
                    list.add(foundData.data)

                    while (list.size < end - start && cursor.getNext(foundKey, foundData, LockMode.DEFAULT) === OperationStatus.SUCCESS) {
                        list.add(foundData.data)
                    }
                }

                list
            })
        }
    }
}