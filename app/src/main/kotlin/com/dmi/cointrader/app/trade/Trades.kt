package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.binance.BinanceClock
import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.binance.binanceClock
import com.dmi.cointrader.app.binance.prodBinanceExchange
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
import kotlinx.coroutines.experimental.channels.*
import org.slf4j.Logger
import java.awt.Toolkit
import java.time.Clock
import java.time.Duration

suspend fun realTrades() = resourceContext {
    val exchange = prodBinanceExchange()
    val config = loadTradeConfig()
    val network = trainedNetwork()
    val history = binanceHistory(exchange)
    val trade = RealTrade(exchange, config, network, history)
    realTrades(exchange, config, trade)
}

suspend fun realTrades(exchange: BinanceExchange, config: TradeConfig, trade: RealTrade) {

}

suspend fun forEachTradePeriod(trade: suspend () -> Unit) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

class RealTradePeriods() {

}

class RealTrades(
        private val exchange: BinanceExchange,
        private val network: NeuralNetwork,
        private val moments: X,
        private val periods: Periods,
        private val log: Logger
) {
    suspend operator fun invoke() {
        periods().consumeEach { period ->
            try {
                tradeAndLog()
            } catch (e: Exception) {
                log.debug("exception", e)
                Toolkit.getDefaultToolkit().beep()
            }
        }
    }

    private fun periods(): ReceiveChannel<Period> = produce {
        var previousPeriod: Period? = null
        while (isActive) {
            val info = preloadInfo()
            val currentTime = info.clock.instant()
            val currentPeriod = periods.of(currentTime)
            val nextPeriod = if (previousPeriod == null) {
                currentPeriod.next()
            } else {
                max(previousPeriod, currentPeriod).next()
            }
            delay(Duration.between(currentTime, periods.startOf(nextPeriod)))
            send(nextPeriod)
            previousPeriod = nextPeriod
        }
    }

    private suspend fun preloadInfo() = PreloadInfo(
            binanceClock(exchange)
    )
}

class PreloadInfo(
        val clock: Clock
)

private suspend fun tradeAndLog() {
    moments.loadBefore(currentTime)

    //                loadInfo(period)
    val assets = exchange.portfolio()
    val capital = capitals.sum()
    log.info("$capital ($capitals)")
}

private suspend fun trade() {
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
}

//fun testTradePeriods(context: PeriodContext, times: InstantRange): ReceiveChannel<Period> {
//    return times.rangeMap(context::periodOf).asSequence().asReceiveChannel()
//}