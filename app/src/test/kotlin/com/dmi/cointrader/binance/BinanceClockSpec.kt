package com.dmi.cointrader.binance

import com.dmi.util.concurrent.delay
import com.dmi.util.lang.millis
import com.dmi.util.test.Spec
import com.dmi.util.lang.minus
import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.should

class BinanceClockSpec : Spec({
    val clock = binanceClock(binanceExchangeForTest())

    "test delay" {
        val t1 = clock.instant()
        delay(millis(100))
        val t2 = clock.instant()
        (t2 - t1) should beGreaterThan(millis(99))
    }
})