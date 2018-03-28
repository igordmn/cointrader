package com.dmi.util.test

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.nio.file.FileSystem

fun testFileSystem(): FileSystem = Jimfs.newFileSystem(Configuration.unix())