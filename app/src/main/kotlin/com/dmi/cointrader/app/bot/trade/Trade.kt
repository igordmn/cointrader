package com.dmi.cointrader.app.bot.trade

import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.PeriodContext
import com.dmi.cointrader.app.candle.asSequence
import com.dmi.util.collection.map
import com.dmi.util.concurrent.delay
import com.dmi.util.lang.InstantRange
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import org.slf4j.Logger
import java.awt.Toolkit
import java.time.Duration
import java.time.Instant

typealias Command<IN> = suspend (IN) -> Unit
typealias Action = suspend () -> Unit
typealias Property<OUT> = suspend () -> OUT
typealias Value<IN, OUT> = suspend (IN) -> OUT

suspend fun performRealTrades(context: PeriodContext, getCurrentTime: Property<Instant>, trade: Action, log: Logger) {
    realTradePeriods(context, getCurrentTime).consumeEach {
        try {
            trade()
        } catch (e: Exception) {
            log.error("error", e)
            Toolkit.getDefaultToolkit().beep()
        }
    }
}

suspend fun performTestTrades(context: PeriodContext, times: InstantRange, trade: Action) {
    testTradePeriods(context, times).consumeEach {
        trade()
    }
}

fun realTradePeriods(context: PeriodContext, getCurrentTime: Property<Instant>): ReceiveChannel<Period> = produce {
    var previousPeriod: Period? = null
    while (isActive) {
        val currentTime = getCurrentTime().apply {
            require(this >= context.start)
        }
        val nextPeriod = context.periodOf(currentTime).next()
        delay(Duration.between(currentTime, context.timeOf(nextPeriod)))
        if (previousPeriod == null || nextPeriod > previousPeriod) {
            send(nextPeriod)
        }
        previousPeriod = nextPeriod
    }
}

fun testTradePeriods(context: PeriodContext, times: InstantRange): ReceiveChannel<Period> {
    return times.map(context::periodOf).asSequence().asReceiveChannel()
}

//fun <PORTFOLIO> trade(
//        portfolio: Property<PORTFOLIO>,
//        rebalance: Command<PORTFOLIO>,
//        bestPortfolio: Value<PORTFOLIO, PORTFOLIO>
//) = action {
//    rebalance(bestPortfolio(portfolio()))
//}