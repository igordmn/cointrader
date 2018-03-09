package com.dmi.cointrader.app.bot.trade

import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.PeriodContext
import com.dmi.cointrader.app.candle.asSequence
import com.dmi.cointrader.main.Portfolio
import com.dmi.util.collection.map
import com.dmi.util.concurrent.delay
import com.dmi.util.lang.InstantRange
import com.dmi.util.lang.indexOfMax
import com.dmi.util.math.portions
import com.dmi.util.math.times
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import old.exchange.Market
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

suspend fun performTestTrades(context: PeriodContext, times: InstantRange, trade: Action) {
    testTradePeriods(context, times).consumeEach {
        trade()
    }
}

fun testTradePeriods(context: PeriodContext, times: InstantRange): ReceiveChannel<Period> {
    return times.map(context::periodOf).asSequence().asReceiveChannel()
}

typealias Capitals = List<Double>

interface Exchange {
    suspend fun capitals(): Capitals
    suspend fun transfer(sellIndex: Int, buyIndex: Int, amount: Double)
}

interface Adviser {
    suspend fun bestPortfolio(current: Portfolio): Portfolio
}

suspend fun trade(exchange: Exchange, adviser: Adviser) {
    val capitals = exchange.capitals()
    val currentPortfolio = capitals.portions()
    val bestPortfolio = adviser.bestPortfolio(currentPortfolio)
    val currentAsset = currentPortfolio.indexOfMax()
    val buyAsset = bestPortfolio.indexOfMax()
    exchange.transfer(currentAsset, buyAsset, capitals[currentAsset])
}