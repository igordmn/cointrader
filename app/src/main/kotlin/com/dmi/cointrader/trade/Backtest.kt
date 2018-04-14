package com.dmi.cointrader.trade

import com.dmi.cointrader.TrainConfig
import com.dmi.cointrader.archive.archive
import com.dmi.cointrader.archive.tradePeriods
import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.info.saveChart
import com.dmi.cointrader.neural.clampForTradedHistory
import com.dmi.cointrader.neural.trainedNetwork
import com.dmi.cointrader.savedTradeConfig
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.io.resourceContext
import com.sun.javafx.application.PlatformImpl
import java.awt.Desktop
import java.nio.file.Files

suspend fun backtest(days: Double) = resourceContext {
    val config = savedTradeConfig()
    val trainConfig = TrainConfig()
    val binanceExchange = binanceExchangeForInfo()
    val testExchange = TestExchange(config.assets, trainConfig.fee.toBigDecimal())
    val network = trainedNetwork()

    val lastPeriod = config.periodSpace.floor(binanceExchange.currentTime())
    val firstPeriod = lastPeriod - (days * config.periodSpace.periodsPerDay()).toInt()

    val archive = archive(
            config.periodSpace, config.assets, binanceExchange,
            lastPeriod,
            reloadCount = config.archiveReloadPeriods
    )
    val periods = (firstPeriod..lastPeriod)
            .clampForTradedHistory(config.historyPeriods, config.tradePeriods.delay)
            .tradePeriods(config.tradePeriods.size)

    val results = performTestTrades(periods, config, network, archive, testExchange)

    val summary = tradeSummary(config.periodSpace, config.tradePeriods.size, results.map { it.totalCapital }, emptyList())
    val file = Files.createTempFile("", "")
    PlatformImpl.startup({})
    saveChart(summary.chartData, file)
    println(summary)
    Desktop.getDesktop().open(file.toFile())
}