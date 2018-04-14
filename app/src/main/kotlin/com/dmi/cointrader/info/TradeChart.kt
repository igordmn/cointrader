package com.dmi.cointrader.info

import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.util.StringConverter
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.log2

class ChartData(val x: DoubleArray, val y: DoubleArray)

fun saveLogChart(data: ChartData, file: Path) {
    Platform.runLater {
        val xAxis = NumberAxis()
        val yAxis = NumberAxis().apply {
            tickLabelFormatter = object: StringConverter<Number>() {
                override fun toString(num: Number): String = "2^$num"
                override fun fromString(string: String): Number = throw UnsupportedOperationException()
            }
        }
        val series = XYChart.Series<Number, Number>().apply {
            this.data.addAll(data.x.zip(data.y) { x, y -> XYChart.Data(x as Number, log2(y) as Number) })
        }
        val chart = LineChart(xAxis, yAxis).apply {
            animated = false
            createSymbols = false
            this.data.add(series)
            isLegendVisible = false
        }
        val scene = Scene(chart, 1600.0, 900.0)
        val image = scene.snapshot(null)

        file.toFile().outputStream().buffered().use {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", it)
        }
    }
}