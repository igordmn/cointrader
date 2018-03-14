package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.binance.BinanceClock
import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.binance.binanceClock
import com.dmi.cointrader.app.binance.productionBinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.Periods
import com.dmi.cointrader.app.history.binanceHistory
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.trainedNetwork
import com.dmi.cointrader.main.*
import com.dmi.util.concurrent.delay
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.indexOfMax
import com.dmi.util.lang.max
import com.dmi.util.log.logger
import kotlinx.coroutines.experimental.NonCancellable.isActive
import java.awt.Toolkit
import java.time.Clock
import java.time.Duration
import java.time.Instant

suspend fun performRealTrades() = resourceContext {
    val exchange = productionBinanceExchange()
    val config = savedTradeConfig()
    val network = trainedNetwork()
    val history = binanceHistory(exchange)
    val realTrades = RealTrades(config, exchange, network, history)
    realTrades()
}

class RealTrades(
        private val config: TradeConfig,
        private val exchange: BinanceExchange,
        private val network: NeuralNetwork,
        private val history: X
) {
    private val log = logger(RealTrades::class)

    suspend operator fun invoke() {
        val periods = Periods(config.startTime, config.period)
        val periodIterator = PeriodIterator(periods)
        while (isActive) {
            val info = preloadInfo()
            val currentTime = info.clock.instant()
            val nextPeriod = periodIterator.nextAfter(currentTime)
            delay(Duration.between(currentTime, periods.startOf(nextPeriod)))
            safeTrade(info)
        }
    }

    private suspend fun preloadInfo() = PreloadedInfo(
            binanceClock(exchange)
    )

    private suspend fun safeTrade(info: PreloadedInfo) = try {
        trade(info)
    } catch (e: Exception) {
        log.debug("exception", e)
        Toolkit.getDefaultToolkit().beep()
    }

    private suspend fun trade(info: PreloadedInfo) {
        val coinAmounts = exchange.portfolio()
        val prices = exchange.prices()
        val capitals = coinAmounts * prices
        val currentPortfolio = capitals.portions()
        val bestPortfolio = bestPortfolio(currentPortfolio)
        val currentCoin = currentPortfolio.indexOfMax()
        val buyCoin = bestPortfolio.indexOfMax()
        exchange.sell(currentCoin, buyCoin, coinAmounts[currentCoin])
    }

    private fun bestPortfolio(currentPortfolio: Portfolio): Portfolio {
        return network.bestPortfolio(currentPortfolio.toMatrix(), TODO()).toPortfolio()
    }

    class PreloadedInfo(
            val clock: Clock
    )
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