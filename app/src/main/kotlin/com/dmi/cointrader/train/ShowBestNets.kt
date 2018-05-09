package com.dmi.cointrader.train

import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import org.apache.commons.io.FileUtils.copyDirectory
import java.nio.file.Files
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths

fun showBestNets(count: Int) {
    data class Info(val result: SavedTrainResult, val dir: Path) {
        override fun toString(): String {
            return "$result ($dir)"
        }
    }

    val dir = Paths.get("data/results")
    val info: List<Info> =
            Files.newDirectoryStream(dir).use {
                it.map { repeatDir ->
                    parseResults(repeatDir.resolve("results.log")).map { result ->
                        Info(result, repeatDir)
                    }
                }
            }.flatten()

    val bestResultsDir = Paths.get("data/resultsBest")
    bestResultsDir.deleteRecursively()
    createDirectories(bestResultsDir)

    val bestInfo = info.sortedByDescending { it.result.test0DayProfitMedian }.take(count)
    bestInfo.forEachIndexed { num, it ->
        val netDir = it.dir.resolve("networks").resolve(it.result.step.toString())
        val chartFile = it.dir.resolve("charts1").resolve(it.result.step.toString() + ".png")


        copyDirectory(netDir.toFile(), bestResultsDir.resolve("net$num").toFile())
        copy(chartFile, bestResultsDir.resolve("$num.png"))
        val resultDir = it.dir
        val resultStr = it.result.str
        bestResultsDir.resolve("results.log").appendLine("$num    $resultStr ($resultDir)")
    }
}
