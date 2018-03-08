package com.dmi.cointrader.app.bot.trade

import com.dmi.cointrader.app.candle.Periods
import com.dmi.util.concurrent.delay
import com.dmi.util.lang.InstantRange
import kotlinx.coroutines.experimental.channels.ReceiveChannel
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

suspend fun performRealTrades(periods: Periods, getCurrentTime: Property<Instant>, trade: Action, log: Logger) {
    realTradeSignals(periods, getCurrentTime).consumeEach {
        try {
            trade()
        } catch (e: Exception) {
            log.error("error", e)
            Toolkit.getDefaultToolkit().beep()
        }
    }
}

suspend fun performTestTrades(periods: Periods, timeRange: InstantRange, trade: Action) {
    testTradeSignals(periods, timeRange).consumeEach {
        trade()
    }
}

fun realTradeSignals(periods: Periods, getCurrentTime: Property<Instant>): ReceiveChannel<Unit> = produce {
    while (isActive) {
        val currentTime = getCurrentTime().apply {
            require(this > periods.startTime)
        }
        val timeForNextTrade = timeForNextTrade(periods, currentTime)
        delay(timeForNextTrade)
        send(Unit)
    }
}

fun testTradeSignals(periods: Periods, timeRange: InstantRange): ReceiveChannel<Unit> = produce {
    var t = timeRange.start + timeForNextTrade(periods, timeRange.start)
    while (t <= timeRange.endInclusive) {
        send(Unit)
        t += periods.period
    }
}

private fun timeForNextTrade(periods: Periods, currentTime: Instant): Duration {
    val periodNum = periods.numOf(currentTime)
    val nextStart = periods.timeOf(periodNum + 1)
    return Duration.between(currentTime, nextStart)
}

fun <PORTFOLIO> trade(
        portfolio: Property<PORTFOLIO>,
        rebalance: Command<PORTFOLIO>,
        bestPortfolio: Value<PORTFOLIO, PORTFOLIO>
) = action {
    rebalance(bestPortfolio(portfolio()))
}