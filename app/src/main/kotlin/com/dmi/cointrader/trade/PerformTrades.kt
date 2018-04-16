package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.*
import com.dmi.cointrader.broker.*
import com.dmi.cointrader.archive.*
import com.dmi.cointrader.TradeConfig
import com.dmi.cointrader.savedTradeConfig
import com.dmi.cointrader.neural.*
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.delay
import com.dmi.util.io.appendLine
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.*
import com.dmi.util.log.rootLog
import kotlinx.coroutines.experimental.NonCancellable.isActive
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.toList
import org.slf4j.Logger
import java.awt.Toolkit
import java.time.Clock
import java.time.Duration
import java.time.Instant
import com.dmi.util.math.div
import com.dmi.util.math.portions
import com.dmi.util.math.times
import java.nio.file.Files.createDirectories
import java.nio.file.Paths
import kotlin.math.abs
import kotlin.math.sqrt

suspend fun performRealTrades() = resourceContext {
    System.setProperty("log4j.configurationFile", "log4j2-realtrade.xml")
    createDirectories(Paths.get("data/log"))
    val log = rootLog()
    val config = savedTradeConfig()
    val network = trainedNetwork()
    val exchange = binanceExchangeForTrade(log)
    val archive = archive(
            config.periodSpace, config.assets, exchange,
            config.periodSpace.floor(exchange.currentTime()),
            reloadCount = config.archiveReloadPeriods
    )

    forEachRealTradePeriod(
            config.periodSpace,
            config.tradePeriods.size,
            config.preloadPeriods,
            {
                try {
                    archive.sync(it)
                } catch (e: Throwable) {
                    log.error("Error on sync", e)
                }
            }
    ) { period ->
        try {
            retry<Unit, Throwable>(attempts = 3) {
                performRealTrade(config, exchange, archive, period, Clock.systemUTC(), network, log)
                delay(seconds(35))
            }
        } catch (e: Throwable) {
            log.error("Error on trade", e)
        }
    }
}

suspend fun forEachRealTradePeriod(
        space: PeriodSpace,
        tradePeriods: Int,
        preloadPeriods: Int,
        preload: suspend (Period) -> Unit,
        action: suspend (Period) -> Unit
) {
    require(preloadPeriods in 0..tradePeriods)

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
        val clock = Clock.systemUTC()
        val nextPeriod = iterator.nextAfter(clock.instant())
        val nextTime = space.timeOf(nextPeriod)

        val preloadPeriod = nextPeriod - preloadPeriods
        val preloadTime = space.timeOf(preloadPeriod)
        delay(preloadTime - clock.instant())
        preload(preloadPeriod)

        delay(nextTime - clock.instant())
        action(nextPeriod)
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
            ?.fileLog(Paths.get("data/log/trades.log"), baseAsset, quoteAsset)

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
    createDirectories(Paths.get("data/log"))
    Paths.get("data/log/capitals.log").appendLine("$time\t$capital")
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

suspend fun performTestTradesAllInFast(
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

suspend fun performTestTradesPartialFast(
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
        val prices = asks.zip(bids) { a, b -> sqrt(a * b) }
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