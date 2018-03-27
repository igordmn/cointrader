package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.binanceExchangeForInfo
import com.dmi.util.test.Spec

class ArchiveSpec : Spec({
    val exchange = binanceExchangeForInfo()
})