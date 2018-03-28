package com.dmi.util.io

import com.dmi.util.test.Spec
import com.dmi.util.test.testFileSystem
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.runBlocking
import java.nio.file.Files
import java.nio.file.Path

class AtomicFileDataStoreSpec : Spec() {
    init {
        "shouldn't exist initially" {
            test { file ->
                val store = AtomicFileDataStore(file)
                store.exists() shouldBe false
            }
        }

        "write and read data" {
            test { file ->
                val store = AtomicFileDataStore(file)

                store.write(byteArrayOf(1, 2, 3))

                store.read().toList() shouldBe byteListOf(1, 2, 3)
            }
        }

        "write twice and read data" {
            test { file ->
                val store = AtomicFileDataStore(file)

                store.write(byteArrayOf(1, 2, 3))
                store.write(byteArrayOf(1, 2))

                store.read().toList() shouldBe byteListOf(1, 2)
            }
        }

        "remove if not exists" {
            test { file ->
                val store = AtomicFileDataStore(file)

                store.exists() shouldBe false
                store.remove()
                store.exists() shouldBe false
            }
        }

        "remove newCandles write" {
            test { file ->
                val store = AtomicFileDataStore(file)

                store.write(byteArrayOf(1, 2, 3))
                store.exists() shouldBe true
                store.remove()
                store.exists() shouldBe false
            }
        }

        "write empty data" {
            test { file ->
                val store = AtomicFileDataStore(file)

                store.write(byteArrayOf())

                store.exists() shouldBe true
                store.read().toList() shouldBe byteListOf()
            }
        }

        "restore data" - {
            "if tempFile exists and main file exists, restore from main file" {
                test { file ->
                    file.append(1, 2)

                    val tempFile = file.appendToFileName(".tmp")
                    tempFile.append(1, 2, 3, 4, 5)

                    val store = AtomicFileDataStore(file)

                    store.exists() shouldBe true
                    store.read().toList() shouldBe byteListOf(1, 2)
                    require(!Files.exists(tempFile))
                }
            }

            "if tempFile exists and main file exists, than write data without restoring" {
                test { file ->
                    file.append(1, 2)

                    val tempFile = file.appendToFileName(".tmp")
                    tempFile.append(1, 2, 3, 4, 5)

                    val store = AtomicFileDataStore(file)
                    store.write(byteArrayOf(1, 2, 6))

                    store.exists() shouldBe true
                    store.read().toList() shouldBe byteListOf(1, 2, 6)
                    require(!Files.exists(tempFile))
                }
            }

            "if tempFile exists and main file doesn't exists, restore from tempFile and remove it" {
                test { file ->
                    require(!Files.exists(file))

                    val tempFile = file.appendToFileName(".tmp")
                    tempFile.append(1, 2, 3, 4, 5)

                    val store = AtomicFileDataStore(file)

                    store.exists() shouldBe true
                    store.read().toList() shouldBe byteListOf(1, 2, 3, 4, 5)
                    require(!Files.exists(tempFile))
                }
            }

            "if tempFile exists than can rewrite data" {
                test { file ->
                    require(!Files.exists(file))

                    val tempFile = file.appendToFileName(".tmp")
                    tempFile.append(1, 2, 3, 4, 5)

                    val store = AtomicFileDataStore(file)
                    store.write(byteArrayOf(1, 2))

                    store.read().toList() shouldBe byteListOf(1, 2)
                    require(!Files.exists(tempFile))
                }
            }
        }
    }

    private suspend fun test(action: suspend (Path) -> Unit) {
        testFileSystem().use {
            action(it.getPath("/test"))
        }
    }

    private fun byteListOf(vararg data: Byte): List<Byte> = data.toList()
}
