package com.dmi.cointrader.app.trade

import com.dmi.cointrader.app.binance.*
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.candle.Periods
import com.dmi.cointrader.app.history.History
import com.dmi.cointrader.app.history.binanceHistory
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.Portions
import com.dmi.cointrader.app.neural.trainedNetwork
import com.dmi.cointrader.app.broker.OrderAttempts
import com.dmi.cointrader.main.*
import com.dmi.util.concurrent.delay
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.indexOfMax
import com.dmi.util.lang.max
import com.dmi.util.math.portions
import com.dmi.util.math.times
import com.dmi.util.math.toDouble
import kotlinx.coroutines.experimental.NonCancellable.isActive
import org.slf4j.Logger
import java.awt.Toolkit
import java.time.Clock
import java.time.Duration
import java.time.Instant

suspend fun performRealTrades() = resourceContext {
    val exchange = productionBinanceExchange()
    val config = savedTradeConfig()
    val network = trainedNetwork()
    val history = binanceHistory(exchange)
    fun realTrade(clock: Clock) = RealTrade(config, exchange, history, clock, network)
    val realTrades = RealTrades(config, exchange, ::realTrade)
    realTrades()
}

suspend fun performTestTrades(config: TradeConfig, network: NeuralNetwork) = resourceContext {

}

class RealTrades(
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

suspend fun realTrade(
        config: TradeConfig,
        exchange: BinanceExchange,
        history: History.Window,
        clock: Clock,
        network: NeuralNetwork,
        log: Logger
) {
    try {

    } catch (e: Exception) {
        log.error("exception", e)
        Toolkit.getDefaultToolkit().beep()
    }
}

interface TradeContext {
    val mainAsset: Asset
    val altAssets: List<Asset>
    suspend fun history(): History.Window
    suspend fun portfolio(): Portfolio
    suspend fun bestPortfolio(current: Portions, history: History.Window): Portions
    suspend fun market(mainAsset: Asset, toAsset: Asset)
}

suspend fun TradeContext.trade() {
    val allAssets = listOf(mainAsset) + altAssets
    val amounts = portfolio()
            .amountsOf(allAssets)
            .toDouble()
    val history = history()
    val prices = history.prices
    val mainAmounts = amounts * prices
    val currentPortions = mainAmounts.portions()
    val bestPortions = bestPortfolio(currentPortions, history)
    val currentIndex = currentPortions.indexOfMax()
    val buyIndex = bestPortions.indexOfMax()
    val currentAsset = allAssets[currentIndex]
    val buyAsset = allAssets[buyIndex]

    val attempts = OrderAttempts(count = 10, amountMultiplier = 0.99)
    val currentCapital = mainAmounts[currentIndex].toBigDecimal()
    if (currentAsset != mainAsset) {
        val currentPrice = prices[currentIndex].toBigDecimal()
        val currentMarket = exchange.market(mainAsset, currentAsset, currentPrice)
        currentMarket.safeBuy(attempts, currentCapital, clock.instant())
    }
    if (currentAsset != mainAsset) {
        val buyPrice = prices[buyIndex].toBigDecimal()
        val buyMarket = exchange.market(mainAsset, buyAsset, buyPrice)
        buyMarket.safeSell(attempts, currentCapital, clock.instant())
    }
}

//fun testTradePeriods(context: PeriodContext, times: InstantRange): ReceiveChannel<Period> {
//    return times.rangeMap(context::periodOf).asSequence().asReceiveChannel()
//}