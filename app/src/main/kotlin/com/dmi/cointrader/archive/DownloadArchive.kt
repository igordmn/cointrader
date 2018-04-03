package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.TradeConfig
import java.nio.file.FileSystems

suspend fun downloadArchive() {
    val config = TradeConfig()
    val exchange = binanceExchangeForInfo()
    archive(
            config.periodSpace,
            config.assets,
            exchange,
            config.periodSpace.floor(exchange.currentTime()),
            FileSystems.getDefault(),
            reloadCount = config.archiveReloadPeriods
    )
}