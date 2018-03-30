package com.dmi.util.io

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun Path.appendToFileName(text: String): Path = resolveSibling(fileName.toString() + text)
fun Path.append(vararg data: Byte): Path = Files.write(this, data, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
fun Path.deleteRecursively() = toFile().deleteRecursively()
fun Path.appendText(text: String, charset: Charset = Charsets.UTF_8) = toFile().appendText(text, charset)
fun Path.appendLine(text: String, charset: Charset = Charsets.UTF_8) = appendText(text + "\n", charset)
fun Path.readBytes(): ByteArray = toFile().readBytes()
fun Path.writeBytes(array: ByteArray) = toFile().writeBytes(array)