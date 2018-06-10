package com.dmi.cointrader.trade

import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.tradePeriods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.info.saveLogChart
import com.dmi.cointrader.neural.clampForTradedHistory
import com.dmi.cointrader.neural.trainedNetwork
import com.dmi.cointrader.savedTradeConfig
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.io.appendLine
import com.dmi.util.io.deleteRecursively
import com.dmi.util.io.resourceContext
import com.sun.javafx.application.PlatformImpl
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Files.createDirectories
import java.nio.file.Paths

suspend fun backtest(daysList: List<Double>) = resourceContext {
    val config = savedTradeConfig()
    val trainConfig = TrainConfig()
    val binanceExchange = binanceExchangeForInfo()
    val testExchange = TestExchange(config.assets, trainConfig.fee.toBigDecimal())
    val network = trainedNetwork()

    val lastPeriod = config.periodSpace.floor(binanceExchange.currentTime())

    val archive = archive(
            config.periodSpace, config.assets, binanceExchange,
            lastPeriod,
            reloadCount = config.archiveReloadPeriods
    )

    val path = Paths.get("data/resultsBest")
    val logPath = path.resolve("results.log")

    path.deleteRecursively()
    createDirectories(path)

    PlatformImpl.startup({})

    for (days in daysList) {
        val firstPeriod = lastPeriod - (days * config.periodSpace.periodsPerDay()).toInt()

        val periods = (firstPeriod..lastPeriod)
                .clampForTradedHistory(config.historyPeriods, config.tradePeriods.delay)
                .tradePeriods(config.tradePeriods.size)

        val results = performTestTrades(periods, config, network, archive, testExchange)
        val summary = tradeSummary(config.periodSpace, config.tradePeriods.size, results.map { it.totalCapital }, emptyList())

        val file = path.resolve("$days.png")
        saveLogChart(summary.chartData, file)
        logPath.appendLine("$days $summary")
        println("$days $summary")
    }
}