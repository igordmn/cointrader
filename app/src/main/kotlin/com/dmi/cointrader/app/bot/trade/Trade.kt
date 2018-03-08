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

interface Fun<in IN, out OUT> {
    suspend operator fun invoke(value: IN): OUT
}

interface Command<in IN> : Fun<IN, Unit> {
    override suspend operator fun invoke(value: IN)
}

interface Action : Command<Unit> {
    override suspend operator fun invoke(value: Unit) = invoke()
    suspend operator fun invoke()
}

interface Value<out OUT> : Fun<Unit, OUT> {
    override suspend operator fun invoke(value: Unit): OUT = invoke()
    suspend operator fun invoke(): OUT
}

fun action(invoke: suspend () -> Unit) = object : Action {
    suspend override fun invoke() = invoke()
}

fun <T> command(invoke: suspend (T) -> Unit) = object : Command<T> {
    suspend override fun invoke(value: T) = invoke(value)
}

fun <T> Command<T>.curry(value: T) = action { invoke(value) }

fun performRealTrades(periods: Periods, getCurrentTime: Value<Instant>, trade: Action, log: Logger) = action {
    realTradeSignals(periods, getCurrentTime).consumeEach {
        try {
            trade()
        } catch (e: Exception) {
            log.error("error", e)
            Toolkit.getDefaultToolkit().beep()
        }
    }
}

fun performTestTrades(periods: Periods, timeRange: InstantRange, trade: Action) = action {
    testTradeSignals(periods, timeRange).consumeEach {
        trade()
    }
}

fun realTradeSignals(periods: Periods, getCurrentTime: Value<Instant>): ReceiveChannel<Unit> = produce {
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
        portfolio: Value<PORTFOLIO>,
        rebalance: Command<PORTFOLIO>,
        bestPortfolio: Fun<PORTFOLIO, PORTFOLIO>
) = action {
    rebalance(bestPortfolio(portfolio()))
}