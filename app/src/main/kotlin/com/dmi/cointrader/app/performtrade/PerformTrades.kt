package com.dmi.cointrader.app.performtrade

import com.dmi.cointrader.app.binance.*
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.Periods
import com.dmi.cointrader.app.history.binanceArchive
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.trainedNetwork
import com.dmi.cointrader.app.candle.asSequence
import com.dmi.cointrader.app.history.Archive
import com.dmi.cointrader.app.test.TestExchange
import com.dmi.cointrader.main.*
import com.dmi.util.collection.rangeMap
import com.dmi.util.concurrent.delay
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.InstantRange
import com.dmi.util.lang.max
import kotlinx.coroutines.experimental.NonCancellable.isActive
import java.time.Duration
import java.time.Instant

suspend fun performRealTrades() = resourceContext {
    if (!askForRealTrade())
        return@resourceContext

    val config = savedTradeConfig()
    val network = trainedNetwork()
    val exchange = productionBinanceExchange()
    val history = binanceArchive(exchange)

    class PeriodIterator(private val periods: Periods) {
        private var previousPeriod = Period(Long.MIN_VALUE)

        fun nextAfter(time: Instant): Period {
            val timePeriod = periods.of(time)
            return max(previousPeriod, timePeriod).next().also {
                previousPeriod = it
            }
        }
    }

    val iterator = PeriodIterator(config.periods)
    while (isActive) {
        val clock = binanceClock(exchange)
        val currentTime = clock.instant()
        val nextPeriod = iterator.nextAfter(currentTime)
        delay(Duration.between(currentTime, config.periods.startOf(nextPeriod)))
        performRealTrade(config, exchange, history, nextPeriod, clock, network)
    }
}

private fun askForRealTrade(): Boolean {
    println("Run trading real money? enter 'yes' if yes")
    val answer = readLine()
    return if (answer != "yes") {
        println("Answer not 'yes', so exit")
        false
    } else {
        true
    }
}

suspend fun performPastTrades(
        config: TradeConfig,
        exchange: TestExchange,
        archive: Archive,
        network: NeuralNetwork,
        times: InstantRange
) {
    times.rangeMap(config.periods::of).asSequence().forEach {
        performTestTrade(config, exchange, archive, it, network)
    }
}