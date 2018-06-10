package com.dmi.cointrader.trade

import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.Period
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.tradePeriods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.info.saveLogChart
import com.dmi.cointrader.neural.NeuralNetwork
import com.dmi.cointrader.neural.clampForTradedHistory
import com.dmi.cointrader.neural.jep
import com.dmi.cointrader.neural.trainedNetwork
import com.dmi.cointrader.savedTradeConfig
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.io.*
import com.sun.javafx.application.PlatformImpl
import kotlinx.serialization.cbor.CBOR
import kotlinx.serialization.cbor.CBOR.Companion.load
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths

suspend fun backtestBest(daysList: List<Double>) = resourceContext {
    val path = Paths.get("data/resultsBest")

    val firstNetPath = path.toFile().listFiles().first { it.name.startsWith("net") }.toPath()

    val config: TradeConfig = load(firstNetPath.resolve("tradeConfig").readBytes())
    val trainConfig = TrainConfig()
    val binanceExchange = binanceExchangeForInfo()
    val jep = jep()

    val lastPeriod = config.periodSpace.floor(binanceExchange.currentTime())
    val archive = archive(
            config.periodSpace, config.assets, binanceExchange,
            lastPeriod,
            reloadCount = config.archiveReloadPeriods
    )
    val backtestPathAll = path.resolve("backtest")
    val logPath = backtestPathAll.resolve("results.log")

    backtestPathAll.deleteRecursively()
    createDirectories(backtestPathAll)

    for (days in daysList) {
        val firstPeriod = lastPeriod - (days * config.periodSpace.periodsPerDay()).toInt()

        val periods = (firstPeriod..lastPeriod)
                .clampForTradedHistory(config.historyPeriods, config.tradePeriods.delay)
                .tradePeriods(config.tradePeriods.size)

        val backtestPath = backtestPathAll.resolve("backtest")

        val f = object {
            suspend fun backtest(num: String, netDir: Path) = resourceContext {
                val network = NeuralNetwork.load(jep, netDir, gpuMemoryFraction = 0.2).use()
                val testExchange = TestExchange(config.assets, trainConfig.fee.toBigDecimal())
                val results = performTestTrades(periods, config, network, archive, testExchange)
                val summary = tradeSummary(config.periodSpace, config.tradePeriods.size, results.map { it.totalCapital }, emptyList())

                val file = backtestPath.resolve("$days $num.png")
                PlatformImpl.startup({})
                saveLogChart(summary.chartData, file)
                logPath.appendLine("$days $num $summary")
            }
        }

        Files.newDirectoryStream(path.resolve("charts1")).use { chartDir ->
            chartDir.forEach {
                val num = it.toFile().nameWithoutExtension
                f.backtest(num, path.resolve("net$num"))
            }
        }
    }
}