package data.slippage

import java.io.File

private val fee = 0.001

fun main(args: Array<String>) {
    val amounts = listOf(
//            "0.0625",
            "0.125"
//            "0.25"
//            "0.5",
//            "1.0"
    )

    class Line(
            val coinAndAmount: String,
            val buySlippage: Double,
            val sellSlippage: Double
    )

    fun geometricMean(list: List<Double>): Double {
        var res = 1.0
        for (item in list) {
            res *= item
        }
        return Math.pow(res, 1.0 / list.size)
    }

    fun geometricMean(lines: List<Line>): Double {
        return geometricMean(lines.map { Math.sqrt(it.buySlippage * it.sellSlippage) })
    }

    val logFile = File("D:\\Development\\Projects\\cointrader\\log.log")
    val resultFile = File("D:\\Development\\Projects\\cointrader\\logSummary.log")
    resultFile.delete()

    val lines = logFile.readLines().map {
        val values = it.split(" \t ")
        val coin = values[0]
        val amount = values[2]
        val buySlippage = values[3]
        val sellSlippage = values[4]
        Line(
                coin + " " + amount,
                buySlippage.toDouble(),
                sellSlippage.toDouble()
        )
    }
    val coinToSlippage = lines.groupBy { it.coinAndAmount }.mapValues { (1 - 1/geometricMean(it.value)) * (1 - fee) }
    coinToSlippage.forEach { coin, slippage ->
        var needPrint = false
        amounts.forEach {
            if (coin.endsWith(it)) needPrint = true
        }
        if (needPrint)
            resultFile.appendText("$coin\t$slippage\n")
    }
}