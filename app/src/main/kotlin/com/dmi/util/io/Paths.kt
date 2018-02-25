package com.dmi.util.io

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun Path.appendToFileName(text: String): Path = resolveSibling(fileName.toString() + ".tmp")

fun Path.append(vararg data: Byte): Path = Files.write(this, data, StandardOpenOption.APPEND, StandardOpenOption.CREATE)