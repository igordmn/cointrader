package com.dmi.util.io

import com.dmi.util.test.Spec
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import kotlinx.coroutines.experimental.runBlocking
import java.nio.file.Path

class SyncFileArraySpec : Spec() {
    init {
        val fs = Jimfs.newFileSystem(Configuration.unix())

        fun test(action: suspend (Path) -> Unit) {
            Jimfs.newFileSystem(Configuration.unix()).use {
                runBlocking {
                    action(it.getPath("/test"))
                }
            }
        }
    }

    private data class TestConfig(val x: String)
}