package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.*
import com.dmi.cointrader.broker.*
import com.dmi.cointrader.archive.*
import com.dmi.cointrader.neural.*
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.collection.SuspendList
import com.dmi.util.concurrent.delay
import com.dmi.util.concurrent.suspend
import com.dmi.util.io.appendText
import com.dmi.util.io.resourceContext
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
import java.nio.file.Files.createDirectories
import java.nio.file.Paths

suspend fun askAndPerformRealTrades() {
    println("Run trading real money? Enter 'yes'")
    while (readLine() != "yes") {
        println("Answer is not 'yes'")
    }
    performRealTrades()
}

suspend fun performRealTrades() = resourceContext {
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

    try {
        archive.sync(period)
        val portfolio = exchange.portfolio(clock)
        val history = neuralHistory(archive, config.historyPeriods, period)
        val tradeTime = config.periodSpace.timeOf(period + config.tradePeriods.delay)
        val timeForTrade = tradeTime - clock.instant()
        if (timeForTrade >= Duration.ZERO) {
            delay(timeForTrade)
        }
        performTrade(config.assets, network, portfolio, history, ::broker)
        val result = realTradeResult(config.assets, exchange, clock)
        log.info(result.toString())
        writeCapital(result,  config, period)
    } catch (e: Exception) {
        log.error("exception", e)
        Toolkit.getDefaultToolkit().beep()
    }
}

private fun writeCapital(result: TradeResult, config: TradeConfig, period: Period) {
    val time = config.periodSpace.timeOf(period)
    val capital = result.totalCapital
    createDirectories(Paths.get("data/logs"))
    Paths.get("data/logs/capitals.log").appendText("$time\t$capital\n")
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
        val asks = tradedHistory.tradeTimeSpreads.map { it.ask }
        val bids = tradedHistory.tradeTimeSpreads.map { it.bid }
        fun askOf(asset: Asset) = asks[indices[asset]!!].toBigDecimal()
        fun bidOf(asset: Asset) = bids[indices[asset]!!].toBigDecimal()
        fun broker(baseAsset: Asset, quoteAsset: Asset) = exchange.broker(baseAsset, quoteAsset, askOf(quoteAsset), bidOf(quoteAsset))
        performTrade(config.assets, network, portfolio, tradedHistory.history, ::broker)
        testTradeResult(config.assets, exchange, bids.withMainAsset())
    }.toList()
}

private fun List<Double>.withMainAsset() = listOf(1.0) + this