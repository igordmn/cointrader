package main.analyze

import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

private val folders = listOf(
        "D:/Development/Projects/cointrader/archive/0003/app/bin/log/archive",
        "D:/Development/Projects/cointrader/archive/0004/app/bin/log/archive"
)

private val realTradePrefixes = listOf("realTrade", "realTest")

fun realTradeMessages(): Sequence<String> = allFiles()
        .filter {
            isRealTradeLog(it)
        }
        .asSequence()
        .map {
            readLog(it)
        }
        .flatten()


private fun allFiles(): List<Path> = folders
        .map { Paths.get(it) }
        .flatten()

private fun isRealTradeLog(path: Path) = realTradePrefixes.any { path.startsWith(it) }

fun readLog(zipPath: Path): List<String> {
    ZipFile(zipPath.toFile()).use { zip ->
        val entry = zip.entries().asSequence().first()
        return zip.getInputStream(entry).bufferedReader().readLines()
    }
}