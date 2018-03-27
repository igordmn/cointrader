package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.cointrader.trade.TradeAssets
import com.dmi.util.test.Spec
import com.dmi.util.test.instant
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.toList

class ArchiveSpec : Spec({
    val space = ;
    val fileSystem = Jimfs.newFileSystem(Configuration.unix())
    val assets = TradeAssets(main = "BTC", alts = listOf("ETH", "NEO", "USDT"))
    val exchange = binanceExchangeForInfo()
    val period = space.floor(instant(2))
    val archive = archive(space, assets, exchange, period, fileSystem, tradeLoadChunk = 20, reloadCount = 1)
    archive.historyAt(2..5).toList() shouldBe listOf()
})