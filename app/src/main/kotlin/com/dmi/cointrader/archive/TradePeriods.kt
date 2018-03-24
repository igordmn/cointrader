package com.dmi.cointrader.archive

import com.dmi.util.math.ceilDiv
import com.dmi.util.math.floorDiv

fun Period.nextTradePeriod(tradePeriods: Int): Period = (this / tradePeriods) * (tradePeriods + 1)

fun PeriodRange.tradePeriods(tradePeriods: Int): IntProgression {
    return (start ceilDiv tradePeriods)..(endInclusive floorDiv tradePeriods) step tradePeriods
}