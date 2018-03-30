package com.dmi.cointrader.trade

import com.dmi.cointrader.test.TestExchange
import com.dmi.util.test.Spec
import java.math.BigDecimal

class PerformTradeSpec : Spec({
    val assets = TradeAssets(main="BTC", alts = listOf("ETH", "LTC"))
    val exchange = TestExchange(assets, BigDecimal.ZERO)

})