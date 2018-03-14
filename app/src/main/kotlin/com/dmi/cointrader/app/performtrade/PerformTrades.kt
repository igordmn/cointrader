package com.dmi.cointrader.app.performtrade

import com.dmi.cointrader.app.binance.*
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.Periods
import com.dmi.cointrader.app.history.binanceHistory
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.trainedNetwork
import com.dmi.cointrader.app.broker.OrderAttempts
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
    fun realTrade(clock: Clock) = RealTrade(config, exchange, history, clock, network)
    val realTrades = PerformRealTrades(config, exchange, ::realTrade)
    realTrades()
}

suspend fun performTestTrades(config: TradeConfig, network: NeuralNetwork) = resourceContext {

}

class PerformRealTrades(
        private val config: TradeConfig,
        private val exchange: BinanceExchange,
        private val trade: suspend (Clock, period: Period) -> Unit
) {
    suspend operator fun invoke() {
        val periods = Periods(config.startTime, config.period)
        val periodIterator = PeriodIterator(periods)
        while (isActive) {
            val clock = binanceClock(exchange)
            val currentTime = clock.instant()
            val nextPeriod = periodIterator.nextAfter(currentTime)
            delay(Duration.between(currentTime, periods.startOf(nextPeriod)))
            trade(clock, nextPeriod)
        }
    }
}

class PeriodIterator(private val periods: Periods) {
    private var previousPeriod = Period(Long.MIN_VALUE)

    fun nextAfter(time: Instant): Period {
        val timePeriod = periods.of(time)
        return max(previousPeriod, timePeriod).next().also {
            previousPeriod = it
        }
    }
}


//fun testTradePeriods(context: PeriodContext, times: InstantRange): ReceiveChannel<Period> {
//    return times.rangeMap(context::periodOf).asSequence().asReceiveChannel()
//}