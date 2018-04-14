package com.dmi.cointrader.binance

import com.dmi.util.concurrent.delay
import com.dmi.util.lang.millis
import com.dmi.util.test.Spec
import com.dmi.util.lang.minus
import com.dmi.util.lang.seconds
import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.beLessThan
import io.kotlintest.matchers.should
import kotlin.math.abs

class BinanceClockSpec : Spec({
    val exchange = binanceExchangeForTest()

    "test delay" {
        val clock = binanceClock(exchange)
        val t1 = clock.instant()
        delay(millis(100))
        val t2 = clock.instant()
        (t2 - t1) should beGreaterThan(millis(99))
    }

    "test time" {
        val clock = binanceClock(exchange)
        delay(seconds(2))
        abs((exchange.currentTime() - clock.instant()).toMillis()) should beLessThan(500L)
    }
})