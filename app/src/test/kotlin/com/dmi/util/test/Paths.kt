package com.dmi.util.test

import com.dmi.util.io.ResourceContext
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

fun testFileSystem(): FileSystem = Jimfs.newFileSystem(Configuration.unix())

fun ResourceContext.tempDirectory(): Path {
    val dir = Files.createTempDirectory("")
    dir.toFile().deleteOnExit()
    AutoCloseable { dir.toFile().deleteRecursively() }.use()
    return dir
}