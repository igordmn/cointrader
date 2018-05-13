package com.dmi.cointrader.train

import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import kotlinx.serialization.cbor.CBOR
import kotlinx.serialization.cbor.CBOR.Companion.load
import kotlinx.serialization.list
import org.apache.commons.io.FileUtils.copyDirectory
import java.nio.file.Files
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths

fun showBestNets(count: Int) {
    data class Info(val result: TrainResult, val dir: Path) {
        override fun toString(): String {
            return "$result ($dir)"
        }

        fun netDir() = dir.resolve("networks").resolve(result.step.toString())
        fun chartFile() = dir.resolve("charts1").resolve(result.step.toString() + ".png")
    }

    val dir = Paths.get("data/results")
    val info: List<Info> =
            Files.newDirectoryStream(dir).use {
                it.map { repeatDir ->
                    val results = load(TrainResult.serializer().list, repeatDir.resolve("results.dump").toFile().readBytes())
                    results.map { result ->
                        Info(result, repeatDir)
                    }
                }
            }.flatten()

    val bestResultsDir = Paths.get("data/resultsBest")
    bestResultsDir.deleteRecursively()
    createDirectories(bestResultsDir)

    val bestInfo = info.sortedByDescending { it.result.tests[0].score }.take(count)
    bestInfo.forEachIndexed { num, it ->
        copyDirectory(it.netDir().toFile(), bestResultsDir.resolve("net$num").toFile())
        copy(it.chartFile(), bestResultsDir.resolve("$num.png"))
        val resultDir = it.dir
        val result = it.result
        bestResultsDir.resolve("results.log").appendLine("$num    $result ($resultDir)")
    }

    Paths.get("network").deleteRecursively()
    copyDirectory(bestInfo.first().netDir().toFile(), Paths.get("network").toFile())
}
