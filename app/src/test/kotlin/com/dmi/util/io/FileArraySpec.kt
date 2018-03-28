package com.dmi.util.io

import com.dmi.util.test.Spec
import com.dmi.util.test.testFileSystem
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.shouldBe

class FileArraySpec: Spec() {
    init {
        "append and get" {
            val path = testFileSystem().getPath("/test")
            val array = FileArray(path, LongFixedSerializer)
            array.append(listOf(1L))
            array.append(listOf(4L, 6L, 8L))

            array.get(0L..0L) shouldBe listOf(1L)
            array.get(1L..1L) shouldBe listOf(4L)
            array.get(3L..3L) shouldBe listOf(8L)
            array.get(1L..2L) shouldBe listOf(4L, 6L)
            array.get(0L..2L) shouldBe listOf(1L, 4L, 6L)
            array.get(1L..3L) shouldBe listOf(4L, 6L, 8L)
            array.get(0L..3L) shouldBe listOf(1L, 4L, 6L, 8L)
        }
    }
}