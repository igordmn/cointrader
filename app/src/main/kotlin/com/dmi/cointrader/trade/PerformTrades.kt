package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.*
import com.dmi.cointrader.broker.*
import com.dmi.cointrader.archive.*
import com.dmi.cointrader.config.TradeConfig
import com.dmi.cointrader.config.savedTradeConfig
import com.dmi.cointrader.neural.*
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.delay
import com.dmi.util.concurrent.suspend
import com.dmi.util.io.appendLine
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.indexOfMax
import com.dmi.util.lang.max
import com.dmi.util.log.rootLog
import kotlinx.coroutines.experimental.NonCancellable.isActive
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.toList
import org.slf4j.Logger
import java.awt.Toolkit
import java.time.Clock
import java.time.Duration
import java.time.Instant
import com.dmi.util.lang.minus
import com.dmi.util.math.div
import com.dmi.util.math.portions
import com.dmi.util.math.times
import java.nio.file.Files.createDirectories
import java.nio.file.Paths
import kotlin.math.abs

suspend fun askAndPerformRealTrades() {
    println("Run trading real money? Enter 'yes'")
    while (readLine() != "yes") {
        println("Answer is not 'yes'")
    }
    performRealTrades()
}

suspend fun performRealTrades() = resourceContext {
    createDirectories(Paths.get("data/logs"))
    val log = rootLog()
    val config = savedTradeConfig()
    val network = trainedNetwork()
    val exchange = binanceExchangeForTrade(log)
    val archive = archive(
            config.periodSpace, config.assets, exchange,
            config.periodSpace.floor(exchange.currentTime()),
            reloadCount = config.archiveReloadPeriods
    )
    val syncClock = suspend { binanceClock(exchange) }

    forEachRealTradePeriod(syncClock, config.periodSpace, config.tradePeriods.size) { clock, period ->
        performRealTrade(config, exchange, archive, period, clock, network, log)
    }
}

suspend fun forEachRealTradePeriod(
        syncClock: suspend () -> Clock,
        space: PeriodSpace,
        tradePeriods: Int,
        action: suspend (Clock, Period) -> Unit
) {
    val iterator = object {
        var previousPeriod = Long.MIN_VALUE

        fun nextAfter(time: Instant): Period {
            val current = space.floor(time)
            val next = current.nextTradePeriod(tradePeriods)
            return max(previousPeriod + 1, next).also {
                previousPeriod = it
            }
        }
    }

    while (isActive) {
        val clock = syncClock()
        val currentTime = clock.instant()
        val nextPeriod = iterator.nextAfter(currentTime)
        val nextTime = space.timeOf(nextPeriod)
        delay(nextTime - currentTime)
        action(clock, nextPeriod)
    }
}

suspend fun performRealTrade(
        config: TradeConfig,
        exchange: BinanceExchange,
        archive: Archive,
        period: Period,
        clock: Clock,
        network: NeuralNetwork,
        log: Logger
) {
    fun broker(baseAsset: Asset, quoteAsset: Asset) = exchange
            .market(baseAsset, quoteAsset)
            ?.broker(clock)
            ?.log(log, baseAsset, quoteAsset)
            ?.fileLog(Paths.get("data/logs/trades.log"), baseAsset, quoteAsset)

    try {
        archive.sync(period)
        val portfolio = exchange.portfolio(clock)
        val history = neuralHistory(archive, config.historyPeriods, period)
        val tradeTime = config.periodSpace.timeOf(period + config.tradePeriods.delay)
        val timeForTrade = tradeTime - clock.instant()
        if (timeForTrade >= Duration.ZERO) {
            delay(timeForTrade)
        }
        performTrade(config.assets, portfolio, history, network, ::broker)
        val result = realTradeResult(config.assets, exchange, clock)
        log.info(result.toString())
        writeCapital(result, config, period)
    } catch (e: Exception) {
        log.error("exception", e)
        Toolkit.getDefaultToolkit().beep()
    }
}

private fun writeCapital(result: TradeResult, config: TradeConfig, period: Period) {
    val time = config.periodSpace.timeOf(period)
    val capital = result.totalCapital
    createDirectories(Paths.get("data/logs"))
    Paths.get("data/logs/capitals.log").appendLine("$time\t$capital")
}

suspend fun performTestTrades(
        periods: PeriodProgression,
        config: TradeConfig,
        network: NeuralNetwork,
        archive: SuspendList<Spreads>,
        exchange: TestExchange
): List<TradeResult> {
    val indices = config.assets.all.withIndex().associate { it.value to it.index }
    return tradedHistories(archive, config.historyPeriods, config.tradePeriods.delay, periods).map { tradedHistory ->
        val portfolio = exchange.portfolio()
        val asks = tradedHistory.tradeTimeSpreads.map { it.ask }.withMainAsset()
        val bids = tradedHistory.tradeTimeSpreads.map { it.bid }.withMainAsset()
        fun askOf(asset: Asset) = asks[indices[asset]!!]
        fun bidOf(asset: Asset) = bids[indices[asset]!!]
        fun broker(baseAsset: Asset, quoteAsset: Asset): Broker? = exchange.broker(
                baseAsset, quoteAsset,
                askOf(baseAsset).toBigDecimal(),
                bidOf(baseAsset).toBigDecimal()
        )
        performTrade(config.assets, portfolio, tradedHistory.history, network, ::broker)
        testTradeResult(config.assets, exchange, bids)
    }.toList()
}

suspend fun performTestTradesFast(
        periods: PeriodProgression,
        config: TradeConfig,
        network: NeuralNetwork,
        archive: SuspendList<Spreads>,
        fee: Double
): List<Capital> {
    val portfolio = (listOf(1.0) + List(config.assets.alts.size) { 0.0 }).toMutableList()

    return tradedHistories(archive, config.historyPeriods, config.tradePeriods.delay, periods).map { tradedHistory ->
        val spreads = tradedHistory.tradeTimeSpreads
        val asks = spreads.map { it.ask }.withMainAsset()
        val bids = spreads.map { it.bid }.withMainAsset()

        val portions = portfolio.portions()
        val bestPortions = network.bestPortfolio(portions, tradedHistory.history)

        val currentIndex = portions.indexOfMax()
        val buyIndex = bestPortions.indexOfMax()

        if (currentIndex != buyIndex) {
            if (currentIndex != 0) {
                val amount = portfolio[currentIndex]
                val sellPrice = bids[currentIndex]
                portfolio[currentIndex] = 0.0
                portfolio[0] = amount * sellPrice * (1 - fee)
            }
            if (buyIndex != 0) {
                val amount = portfolio[0]
                val buyPrice = asks[buyIndex]
                portfolio[0] = 0.0
                portfolio[buyIndex] = amount / buyPrice * (1 - fee)
            }
        }

        (portfolio * bids).sum()
    }.toList()
}

suspend fun performTestTradesFast2(
        periods: PeriodProgression,
        config: TradeConfig,
        network: NeuralNetwork,
        archive: SuspendList<Spreads>,
        fee: Double
): List<Capital> {
    val portfolio = (listOf(1.0) + List(config.assets.alts.size) { 0.0 }).toMutableList()

    return tradedHistories(archive, config.historyPeriods, config.tradePeriods.delay, periods).map { tradedHistory ->
        val spreads = tradedHistory.tradeTimeSpreads
        val asks = spreads.map { it.ask }.withMainAsset()
        val bids = spreads.map { it.bid }.withMainAsset()
        val prices = asks.zip(bids) { a, b -> (a + b) / 2.0 }
        val fees = bids.zip(prices) { a, b -> 1 - a / b * (1.0 - fee) }

        val portfolioBtc = portfolio * prices
        val portions = portfolioBtc.portions()
        val bestPortions = network.bestPortfolio(portions, tradedHistory.history).portions()

        val capital = portfolioBtc.sum()
        val desiredPortfolio = bestPortions * capital

        val totalFee = (desiredPortfolio.zip(portfolioBtc) { a, b -> abs(a - b) }.drop(1) * fees.drop(1)).sum()
        val capitalAfterFee = capital - totalFee

        val newPortfolioBtc = bestPortions * capitalAfterFee
        val newPortfolio = newPortfolioBtc / prices

        portfolio.indices.forEach {
            portfolio[it] = newPortfolio[it]
        }

        capitalAfterFee
    }.toList()
}

suspend fun performTestTradesFast3(
        periods: PeriodProgression,
        config: TradeConfig,
        network: NeuralNetwork,
        archive: SuspendList<Spreads>,
        fee: Double
): List<Capital> {
    val histories = tradedHistories(archive, config.historyPeriods, config.tradePeriods.delay, periods).toList()
    val portfolio = (listOf(1.0) + List(config.assets.alts.size) { 0.0 }).toMutableList()
    val bestPorfolioA = List(histories.size) { emptyList<Double>() }.toMutableList()
    bestPorfolioA.indices.forEach {
        val previous = if (it == 0) {
            portfolio
        } else {
            bestPorfolioA[it - 1]
        }
        val best = network.bestPortfolio(previous, histories[it].history)
        bestPorfolioA[it] = best
    }
    var bestPorfolio = bestPorfolioA.toList()

    val asks = histories.map { listOf(1.0) + it.tradeTimeSpreads.map { it.ask } }
    val bids = histories.map { listOf(1.0) + it.tradeTimeSpreads.map { it.bid } }
    val prices = asks.zip(bids) { a, b -> a.zip(b) { c, d -> (c + d) / 2.0 } }

    var fees = bids.zip(prices) { a, b -> a.zip(b) { c, d -> 1 - (1.0 - fee) * (c / d) } }
    var priceIncs = prices.zipWithNext { c, n -> n / c }
    fees = fees.dropLast(1)
    bestPorfolio = bestPorfolio.dropLast(1)
    var futurePortfolio = priceIncs.zip(bestPorfolio) { a, b -> a * b / (a * b).sum() }

    priceIncs = priceIncs.drop(1)
    fees = fees.drop(1)
    bestPorfolio = bestPorfolio.drop(1)
    val current_portfolio = futurePortfolio.dropLast(1)
    val diff = bestPorfolio.zip(current_portfolio) { a, b -> (a - b).map { abs(it) } }
    val cost = diff.zip(fees) { a, b -> 1 - (a * b).drop(1).sum() }
    val profit = priceIncs.zip(bestPorfolio) { a, b -> (a * b).sum() }
    return profit.zip(cost) { a, b -> a * b }
}