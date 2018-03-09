package com.dmi.cointrader.app.bot.trade

import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.PeriodContext
import com.dmi.cointrader.app.candle.asSequence
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.main.History
import com.dmi.cointrader.main.Portfolio
import com.dmi.cointrader.main.toMatrix
import com.dmi.cointrader.main.toPortfolio
import com.dmi.util.collection.map
import com.dmi.util.concurrent.delay
import com.dmi.util.lang.InstantRange
import com.dmi.util.lang.indexOfMax
import com.dmi.util.lang.unsupportedOperation
import com.dmi.util.math.DoubleMatrix2D
import com.dmi.util.math.portions
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import old.exchange.candle.Candle
import org.slf4j.Logger
import java.awt.Toolkit
import java.math.BigDecimal
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

interface Market {
    suspend fun buy(amount: Double)
}

interface Exchange {
    suspend fun capitals(): Capitals
    suspend fun transfer(fromIndex: Int, toIndex: Int, amount: Double)
}

suspend fun trade(
        portfolio: Property<Portfolio>,
        exchange: Exchange,
        bestPortfolio: Value<Portfolio, Portfolio>
) {
    allinRebalance(exchange, bestPortfolio(portfolio()))
}

suspend fun allinRebalance(
        exchange: Exchange,
        newPortfolio: Portfolio
) {
    val capitals = exchange.capitals()
    val portfolio = capitals.portions()
    val currentCoin = portfolio.indexOfMax()!!
    val buyCoin = newPortfolio.indexOfMax()!!
    exchange.transfer(currentCoin, buyCoin, capitals[currentCoin])
}