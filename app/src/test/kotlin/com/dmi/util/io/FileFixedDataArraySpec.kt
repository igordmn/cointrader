package com.dmi.util.io

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.runBlocking
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class FileFixedDataArraySpec : FreeSpec() {
    init {
        "initial size" {
            test { file ->
                val array = FileFixedDataArray(file, 5)

                array.size shouldBe 0L
            }
        }

        "append" - {
            "append single item" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBuffer = buffer(1, 2, 3, 4, 5)
                    val readBuffer = ByteBuffer.allocate(5)

                    array.append(writeBuffer)
                    array.read(0L..1L, readBuffer)

                    array.size shouldBe 1L
                    data(readBuffer) shouldBe byteListOf(1, 2, 3, 4, 5)
                }
            }

            "append two items at once" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBufferAll = buffer(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    val readBufferAll = ByteBuffer.allocate(5 * 2)
                    val readBuffer1 = ByteBuffer.allocate(5)
                    val readBuffer2 = ByteBuffer.allocate(5)

                    array.append(writeBufferAll)
                    array.read(0L..2L, readBufferAll)
                    array.read(0L..1L, readBuffer1)
                    array.read(1L..2L, readBuffer2)

                    array.size shouldBe 2L
                    data(readBufferAll) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    data(readBuffer1) shouldBe byteListOf(1, 2, 3, 4, 5)
                    data(readBuffer2) shouldBe byteListOf(6, 7, 8, 9, 10)
                }
            }

            "append two items two times" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBuffer1 = buffer(1, 2, 3, 4, 5)
                    val writeBuffer2 = buffer(6, 7, 8, 9, 10)
                    val readBufferAll = ByteBuffer.allocate(5 * 2)
                    val readBuffer1 = ByteBuffer.allocate(5)
                    val readBuffer2 = ByteBuffer.allocate(5)

                    array.append(writeBuffer1)
                    array.append(writeBuffer2)
                    array.read(0L..2L, readBufferAll)
                    array.read(0L..1L, readBuffer1)
                    array.read(1L..2L, readBuffer2)

                    array.size shouldBe 2L
                    data(readBufferAll) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    data(readBuffer1) shouldBe byteListOf(1, 2, 3, 4, 5)
                    data(readBuffer2) shouldBe byteListOf(6, 7, 8, 9, 10)
                }
            }
        }

        "clear" - {
            "clear empty array" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)

                    array.clear()

                    array.size shouldBe 0L
                }
            }

            "clear array" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBuffer = buffer(1, 2, 3, 4, 5)

                    array.append(writeBuffer)
                    array.clear()

                    array.size shouldBe 0L
                }
            }

            "append item after clear" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBuffer = buffer(1, 2, 3, 4, 5)
                    val readBuffer = ByteBuffer.allocate(5)

                    array.clear()
                    array.append(writeBuffer)
                    array.read(0L..1L, readBuffer)

                    array.size shouldBe 1L
                    data(readBuffer) shouldBe byteListOf(1, 2, 3, 4, 5)
                }
            }
        }

        "restrictions" - {
            "cannot append item not multiplied by 5" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBuffer = buffer(1, 2, 3, 4)

                    shouldThrow<IllegalArgumentException> {
                        array.append(writeBuffer)
                    }
                }
            }

            "cannot read item not multiplied by 5" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBuffer = buffer(1, 2, 3, 4, 5)
                    val readBuffer = ByteBuffer.allocate(4)

                    array.clear()
                    array.append(writeBuffer)

                    shouldThrow<IllegalArgumentException> {
                        array.read(0L..1L, readBuffer)
                    }
                }
            }
        }

        "corrupted file" - {
            "read zero items from corrupted file" {
                test { file ->
                    file.append(1, 2, 3, 4)  // not multiplied by 5

                    val array = FileFixedDataArray(file, 5)

                    array.size shouldBe 0L
                }
            }

            "read single item from corrupted file" {
                test { file ->
                    file.append(1, 2, 3, 4, 5, 6)  // not multiplied by 5

                    val array = FileFixedDataArray(file, 5)
                    val readBuffer = ByteBuffer.allocate(5)

                    array.read(0L..1L, readBuffer)

                    array.size shouldBe 1L
                    data(readBuffer) shouldBe byteListOf(1, 2, 3, 4, 5)
                }
            }

            "read two items from corrupted file" {
                test { file ->
                    file.append(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)  // not multiplied by 5

                    val array = FileFixedDataArray(file, 5)
                    val readBufferAll = ByteBuffer.allocate(5 * 2)
                    val readBuffer1 = ByteBuffer.allocate(5)
                    val readBuffer2 = ByteBuffer.allocate(5)

                    array.read(0L..2L, readBufferAll)
                    array.read(0L..1L, readBuffer1)
                    array.read(1L..2L, readBuffer2)

                    array.size shouldBe 2L
                    data(readBufferAll) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    data(readBuffer1) shouldBe byteListOf(1, 2, 3, 4, 5)
                    data(readBuffer2) shouldBe byteListOf(6, 7, 8, 9, 10)
                }
            }

            "append item to corrupted file with two items" {
                test { file ->
                    file.append(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)  // not multiplied by 5

                    val array = FileFixedDataArray(file, 5)
                    val writeBufferAll = buffer(11, 12, 13, 14, 15)
                    val readBufferAll = ByteBuffer.allocate(5 * 3)
                    val readBuffer1And2 = ByteBuffer.allocate(5 * 2)
                    val readBuffer3 = ByteBuffer.allocate(5)

                    array.append(writeBufferAll)
                    array.read(0L..3L, readBufferAll)
                    array.read(0L..2L, readBuffer1And2)
                    array.read(2L..3L, readBuffer3)

                    array.size shouldBe 3L
                    data(readBufferAll) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
                    data(readBuffer1And2) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    data(readBuffer3) shouldBe byteListOf(11, 12, 13, 14, 15)
                }
            }

            "append two items to corrupted file with two items" {
                test { file ->
                    file.append(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)  // not multiplied by 5

                    val array = FileFixedDataArray(file, 5)
                    val writeBufferAll = buffer(11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
                    val readBufferAll = ByteBuffer.allocate(5 * 4)
                    val readBuffer1And2 = ByteBuffer.allocate(5 * 2)
                    val readBuffer3 = ByteBuffer.allocate(5)
                    val readBuffer4 = ByteBuffer.allocate(5)

                    array.append(writeBufferAll)
                    array.read(0L..4L, readBufferAll)
                    array.read(0L..2L, readBuffer1And2)
                    array.read(2L..3L, readBuffer3)
                    array.read(3L..4L, readBuffer4)

                    array.size shouldBe 4L
                    data(readBufferAll) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
                    data(readBuffer1And2) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    data(readBuffer3) shouldBe byteListOf(11, 12, 13, 14, 15)
                    data(readBuffer4) shouldBe byteListOf(16, 17, 18, 19, 20)
                }
            }

            "append two items two times to corrupted file with two items" {
                test { file ->
                    file.append(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)  // not multiplied by 5

                    val array = FileFixedDataArray(file, 5)
                    val writeBuffer1 = buffer(11, 12, 13, 14, 15)
                    val writeBuffer2 = buffer(16, 17, 18, 19, 20)
                    val readBufferAll = ByteBuffer.allocate(5 * 4)
                    val readBuffer1And2 = ByteBuffer.allocate(5 * 2)
                    val readBuffer3 = ByteBuffer.allocate(5)
                    val readBuffer4 = ByteBuffer.allocate(5)

                    array.append(writeBuffer1)
                    array.append(writeBuffer2)
                    array.read(0L..4L, readBufferAll)
                    array.read(0L..2L, readBuffer1And2)
                    array.read(2L..3L, readBuffer3)
                    array.read(3L..4L, readBuffer4)

                    array.size shouldBe 4L
                    data(readBufferAll) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
                    data(readBuffer1And2) shouldBe byteListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    data(readBuffer3) shouldBe byteListOf(11, 12, 13, 14, 15)
                    data(readBuffer4) shouldBe byteListOf(16, 17, 18, 19, 20)
                }
            }
        }

        "modify size" - {
            "modify size and read" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBufferAll = buffer(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    val readBufferAll = ByteBuffer.allocate(5)

                    array.append(writeBufferAll)
                    array.reduceSize(1)
                    array.read(0L..1L, readBufferAll)

                    array.size shouldBe 1L
                    data(readBufferAll) shouldBe byteListOf(1, 2, 3, 4, 5)
                }
            }

            "modify size, append and read" {
                test { file ->
                    val array = FileFixedDataArray(file, 5)
                    val writeBuffer1 = buffer(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    val writeBuffer2 = buffer(11, 12, 13, 14, 15)
                    val readBufferAll = ByteBuffer.allocate(10)

                    array.append(writeBuffer1)
                    array.reduceSize(1)
                    array.append(writeBuffer2)
                    array.read(0L..2L, readBufferAll)

                    array.size shouldBe 2L
                    data(readBufferAll) shouldBe byteListOf(1, 2, 3, 4, 5, 11, 12, 13, 14, 15)
                }
            }
        }
    }

    private fun test(action: suspend (Path) -> Unit) {
        val file = Files.createTempFile("test", "")
        try {
            runBlocking {
                action(file)
            }
        } finally {
            if (Files.exists(file)) {
                Files.delete(file)
            }
        }
    }

    private fun buffer(vararg data: Byte): ByteBuffer {
        val buffer = ByteBuffer.allocate(data.size)
        buffer.put(data)
        buffer.rewind()
        return buffer
    }

    private fun data(buffer: ByteBuffer): List<Byte> {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data.toList()
    }

    private fun byteListOf(vararg data: Byte): List<Byte> = data.toList()
}