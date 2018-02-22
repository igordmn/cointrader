package com.dmi.cointrader.app.trade

import java.time.Instant

data class Trade(val time: Instant, val amount: Double, val price: Double)