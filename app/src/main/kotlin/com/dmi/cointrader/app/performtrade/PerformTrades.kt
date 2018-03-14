package com.dmi.cointrader.app.performtrade

import com.dmi.cointrader.app.binance.*
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.Periods
import com.dmi.cointrader.app.history.binanceHistory
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.trainedNetwork
import com.dmi.cointrader.app.broker.OrderAttempts
import com.dmi.cointrader.app.history.History
import com.dmi.cointrader.main.*
import com.dmi.util.concurrent.delay
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.max
import kotlinx.coroutines.experimental.NonCancellable.isActive
import java.time.Clock
import java.time.Duration
import java.time.Instant

suspend fun performRealTrades() = resourceContext {
    val exchange = productionBinanceExchange()
    val config = savedTradeConfig()
    val network = trainedNetwork()
    val history = binanceHistory(exchange)

    object : PerformPeriodicTradesContext {
        override val periods = config.periods
        suspend override fun syncClock(): Clock = binanceClock(exchange)
        suspend override fun trade(clock: Clock, period: Period) = performRealTrade(
                config, exchange, history.window(period, config.historyCount), clock, network
        )
    }.performPeriodicTrades()
}

suspend fun performTestTrades(config: TradeConfig, network: NeuralNetwork) = resourceContext {

}

interface PerformPeriodicTradesContext {
    val periods: Periods
    suspend fun syncClock(): Clock
    suspend fun trade(clock: Clock, period: Period)
}

suspend fun PerformPeriodicTradesContext.performPeriodicTrades() {
    class PeriodIterator(private val periods: Periods) {
        private var previousPeriod = Period(Long.MIN_VALUE)

        fun nextAfter(time: Instant): Period {
            val timePeriod = periods.of(time)
            return max(previousPeriod, timePeriod).next().also {
                previousPeriod = it
            }
        }
    }

    val iterator = PeriodIterator(periods)
    while (isActive) {
        val clock = syncClock()
        val currentTime = clock.instant()
        val nextPeriod = iterator.nextAfter(currentTime)
        delay(Duration.between(currentTime, periods.startOf(nextPeriod)))
        trade(clock, nextPeriod)
    }
}


//fun testTradePeriods(context: PeriodContext, times: InstantRange): ReceiveChannel<Period> {
//    return times.rangeMap(context::periodOf).asSequence().asReceiveChannel()
//}