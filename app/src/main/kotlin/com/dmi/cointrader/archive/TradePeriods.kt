package com.dmi.cointrader.archive

import com.dmi.util.math.ceilDiv
import com.dmi.util.math.floorDiv

fun Period.nextTradePeriod(tradePeriods: Int): Period = (this / tradePeriods) * (tradePeriods + 1)

fun PeriodRange.tradePeriods(tradeSize: Int): PeriodProgression {
    return tradeSize * (start ceilDiv tradeSize)..tradeSize * (endInclusive floorDiv tradeSize) step tradeSize
}