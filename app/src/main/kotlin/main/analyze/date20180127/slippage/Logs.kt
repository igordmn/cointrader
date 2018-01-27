package main.analyze.date20180127.slippage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

private val folders = listOf(
//        "D:/Development/Projects/cointrader/archive/0003/app/bin/log/archive",
        "D:/Development/Projects/cointrader/archive/0004/app/bin/log/archive"
)

private val realTradePrefixes = listOf("realTrade", "realTest")

fun realTradeLines(): Sequence<String> = allFiles()
        .filter {
            isRealTradeLog(it)
        }
        .asSequence()
        .map {
            readLog(it)
        }
        .flatten()


private fun allFiles(): List<Path> = folders
        .map { Files.newDirectoryStream(Paths.get(it)).toList() }
        .flatten()

private fun isRealTradeLog(path: Path) = realTradePrefixes.any { path.fileName.toString().startsWith(it) }

fun readLog(zipPath: Path): List<String> {
    ZipFile(zipPath.toFile()).use { zip ->
        val entry = zip.entries().asSequence().first()
        return zip.getInputStream(entry).bufferedReader().readLines()
    }
}