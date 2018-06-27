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

suspend fun backtestBest(maxDays: Double, fee: Double?) {
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
    val backtestPath = path.resolve("backtest")
    val logPath = backtestPath.resolve("results.log")

    backtestPath.deleteRecursively()
    createDirectories(backtestPath)

    val firstPeriod = lastPeriod - (maxDays * config.periodSpace.periodsPerDay()).toInt()

    val periods = (firstPeriod..lastPeriod)
            .clampForTradedHistory(config.historyPeriods, config.tradePeriods.delay)
            .tradePeriods(config.tradePeriods.size)

    Files.newDirectoryStream(path.resolve("charts1")).use { chartDir ->
        chartDir.forEach {
            resourceContext {
                val num = it.toFile().nameWithoutExtension

                val network = NeuralNetwork.load(jep, path.resolve("net$num"), gpuMemoryFraction = 0.2).use()
                val testExchange = TestExchange(config.assets, (fee ?: trainConfig.fee).toBigDecimal())
                var results = performTestTrades(periods, config, network, archive, testExchange)

                var days = maxDays
                while (days >= 2) {
                    val summary = tradeSummary(config.periodSpace, config.tradePeriods.size, results.map { it.totalCapital }, emptyList())

                    val file = backtestPath.resolve("$days $num.png")
                    PlatformImpl.startup({})
                    saveLogChart(summary.chartData, file)
                    logPath.appendLine("$days $num $summary")

                    days /= 2
                    results = results.drop(results.size / 2).divideByFirstCapital()
                }
            }
        }
    }
}