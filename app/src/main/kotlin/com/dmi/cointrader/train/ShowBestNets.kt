package com.dmi.cointrader.train

import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
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
        fun chart1File() = dir.resolve("charts1").resolve(result.step.toString() + ".png")
        fun chart2File() = dir.resolve("charts2").resolve(result.step.toString() + ".png")
    }

    val dir = Paths.get("data/results")
    val info: List<Info> =
            Files.newDirectoryStream(dir).use {
                it.filter { Files.exists(it.resolve("results.dump")) }.map { repeatDir ->
                    val results = load(TrainResult.serializer().list, repeatDir.resolve("results.dump").toFile().readBytes())
                    results.map { result ->
                        Info(result, repeatDir)
                    }
                }
            }.flatten()

    val bestResultsDir = Paths.get("data/resultsBest")
    bestResultsDir.deleteRecursively()
    val charts1Dir = bestResultsDir.resolve("charts1")
    val charts2Dir = bestResultsDir.resolve("charts2")
    createDirectories(bestResultsDir)
    createDirectories(charts1Dir)
    createDirectories(charts2Dir)

    val bestInfo = info.sortedByDescending { it.result.tests[0].score2 }.take(count)
    bestInfo.forEachIndexed { num, it ->
        copyDirectory(it.netDir().toFile(), bestResultsDir.resolve("net$num").toFile())
        copy(it.chart1File(), charts1Dir.resolve("$num.png"))
        copy(it.chart2File(), charts2Dir.resolve("$num.png"))
        val resultDir = it.dir
        val result = it.result
        bestResultsDir.resolve("results.log").appendLine("$num    $result ($resultDir)")
    }

    Paths.get("network").deleteRecursively()
    copyDirectory(bestInfo.first().netDir().toFile(), Paths.get("network").toFile())
}
