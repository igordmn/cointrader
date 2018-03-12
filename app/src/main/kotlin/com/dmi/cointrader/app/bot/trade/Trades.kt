package com.dmi.cointrader.app.bot.trade

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.PeriodContext
import com.dmi.cointrader.app.moment.Moment
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.main.Portfolio
import com.dmi.cointrader.main.toMatrix
import com.dmi.cointrader.main.toPortfolio
import com.dmi.util.concurrent.delay
import com.dmi.util.io.SyncList
import com.dmi.util.lang.indexOfMax
import kotlinx.coroutines.experimental.channels.*
import org.slf4j.Logger
import java.awt.Toolkit
import java.time.Duration
import java.time.Instant

typealias CoinAmounts = List<Double>
typealias Prices = List<Double>

class RealTrades(
        private val exchange: BinanceExchange,
        private val network: NeuralNetwork,
        private val moments: SyncList<Moment>,
        private val periodContext: PeriodContext,
        private val log: Logger
) {
    suspend operator fun invoke() {
        periods().consumeEach { period ->
            try {
//                loadInfo(period)
                val coins = exchange.assets()
                val capital = capitals.sum()
                log.info("$capital ($capitals)")
            } catch (e: Exception) {
                log.debug("exception", e)
                Toolkit.getDefaultToolkit().beep()
            }
        }
    }

    private fun periods(): ReceiveChannel<Period> = produce {
        var previousPeriod: Period? = null
        while (isActive) {
            val currentTime = exchange.currentTime().apply {
                require(this >= periodContext.start)
            }
            val nextPeriod = periodContext.periodOf(currentTime).next()
            delay(Duration.between(currentTime, periodContext.timeOf(nextPeriod)))
            if (previousPeriod == null || nextPeriod > previousPeriod) {
                send(nextPeriod)
            }
            previousPeriod = nextPeriod
        }
    }

    private suspend fun trade() {
        val coinAmounts = exchange.assets()
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

    interface Exchange {
        suspend fun load()
        suspend fun currentTime(): Instant
        suspend fun coinAmounts(): CoinAmounts
        suspend fun prices(): Prices
        suspend fun sell(fromIndex: Int, toIndex: Int, amount: Double)
    }
}

//fun testTradePeriods(context: PeriodContext, times: InstantRange): ReceiveChannel<Period> {
//    return times.rangeMap(context::periodOf).asSequence().asReceiveChannel()
//}