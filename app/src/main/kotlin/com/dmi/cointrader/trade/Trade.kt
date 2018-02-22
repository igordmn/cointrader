package com.dmi.cointrader.trade

import java.time.Instant

data class Trade(val time: Instant, val amount: Double, val price: Double)