package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.*
import com.dmi.cointrader.broker.*
import com.dmi.cointrader.archive.*
import com.dmi.cointrader.neural.*
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.concurrent.delay
import com.dmi.util.io.resourceContext
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.MILLIS_PER_HOUR
import com.dmi.util.lang.indexOfMax
import com.dmi.util.lang.max
import com.dmi.util.log.rootLog
import com.dmi.util.math.portions
import com.dmi.util.math.product
import com.dmi.util.math.times
import com.dmi.util.math.toDouble
import kotlinx.coroutines.experimental.NonCancellable.isActive
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.toList
import org.slf4j.Logger
import java.awt.Toolkit
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.pow
import com.dmi.util.lang.minus

suspend fun performRealTrades() = resourceContext {
    askForRealTrade()

    val log = rootLog()
    val config = savedTradeConfig()
    val network = trainedNetwork()
    val exchange = privateBinanceExchange(log)
    val history = archive(config, exchange, config.periodSpace.of(exchange.currentTime()))

    val iterator = object {
        var previousPeriod = Int.MIN_VALUE

        fun nextAfter(time: Instant): Period {
            val current = config.periodSpace.of(time)
            val next = current.nextTradePeriod(config.tradePeriods)
            return max(previousPeriod + 1, next).also {
                previousPeriod = it
            }
        }
    }

    while (isActive) {
        val clock = binanceClock(exchange)
        val currentTime = clock.instant()
        val nextPeriod = iterator.nextAfter(currentTime)
        val nextTime = config.periodSpace.timeOf(nextPeriod).also {
            require(it >= currentTime)
        }
        delay(nextTime - currentTime)
        performRealTrade(config, exchange, history, nextPeriod, clock, network, log)
    }
}

private fun askForRealTrade() {
    println("Run trading real money? Enter 'yes'")
    while (readLine() != "yes") {
        println("Answer is not 'yes'")
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
        val history = neuralHistory(config, archive, period)
        val tradeTime = config.periodSpace.timeOf(period + config.tradeDelayPeriods)
        val timeForTrade = tradeTime - clock.instant()
        if (timeForTrade >= Duration.ZERO) {
            delay(timeForTrade)
        }
        performTrade(config.assets, network, exchange.portfolio(clock), history, ::broker)
        val result = realTradeResult(config.assets, exchange, clock)
        log.info(result.toString())
    } catch (e: Exception) {
        log.error("exception", e)
        Toolkit.getDefaultToolkit().beep()
    }
}

private suspend fun realTradeResult(assets: TradeAssets, exchange: BinanceExchange, clock: Clock): TradeResult {
    val resultAsset = "BTC"
    val minBtc = 0.0001
    val portfolio = exchange.portfolio(clock)
    val btcPrices = exchange.btcPrices()
    val assetCapitals = assets.all
            .associate {
                val capital = if (it == resultAsset) {
                    portfolio[it]!!
                } else {
                    portfolio[it]!! * btcPrices[it]!!
                }
                it to capital.toDouble()
            }
            .filter { it.value > minBtc }
    val totalCapital = assetCapitals.values.sum()
    return TradeResult(assetCapitals, totalCapital, resultAsset)
}

private fun testTradeResult(assets: TradeAssets, exchange: TestExchange, bids: List<Double>): TradeResult {
    val resultAsset = "BTC"
    val minBtc = 0.0001
    val portfolio = exchange.portfolio()
    val amounts = portfolio.amountsOf(assets.all).toDouble()
    val capitals = bids * amounts
    val assetCapitals = assets.all.withIndex().associate {
        it.value to capitals[it.index]
    }.filter {
        it.value > minBtc
    }
    val totalCapital = capitals.sum()
    return TradeResult(assetCapitals, totalCapital, resultAsset)
}

suspend fun performTestTrades(
        range: PeriodRange,
        config: TradeConfig,
        network: NeuralNetwork,
        archive: Archive,
        exchange: TestExchange
): List<TradeResult> {
    val indices = config.assets.all.withIndex().associate { it.value to it.index }
    return tradedHistories(config, archive, range).map { tradedHistory ->
        val portfolio = exchange.portfolio()
        val asks = tradedHistory.tradeTimeSpreads.map { it.ask }
        val bids = tradedHistory.tradeTimeSpreads.map { it.bid }
        fun askOf(asset: Asset) = asks[indices[asset]!!].toBigDecimal()
        fun bidOf(asset: Asset) = bids[indices[asset]!!].toBigDecimal()
        fun broker(baseAsset: Asset, quoteAsset: Asset) = exchange.broker(baseAsset, quoteAsset, askOf(quoteAsset), bidOf(quoteAsset))
        performTrade(config.assets, network, portfolio, tradedHistory.history, ::broker)
        testTradeResult(config.assets, exchange, bids)
    }.toList()
}

suspend fun performTrade(
        assets: TradeAssets,
        network: NeuralNetwork,
        portfolio: Portfolio,
        history: NeuralHistory,
        broker: (baseAsset: Asset, quoteAsset: Asset) -> Broker?
) = with(object {
    suspend fun sell(altAsset: Asset, altPrice: BigDecimal, mainAmount: BigDecimal) {
        brokerFrom(altAsset, altPrice).buy(mainAmount)
    }

    suspend fun buy(altAsset: Asset, altPrice: BigDecimal, mainAmount: BigDecimal) {
        brokerFrom(altAsset, altPrice).sell(mainAmount)
    }

    // todo maybe use indices? it is faster for tests. must profile tests
    fun brokerFrom(altAsset: Asset, altPrice: BigDecimal): Broker {
        val attempts = SafeBroker.Attempts(count = 10, amountMultiplier = 0.99)
        val normalBroker = broker(assets.main, altAsset)
        val reversedBroker = broker(altAsset, assets.main)

        return if (normalBroker != null) {
            normalBroker.safe(attempts)
        } else {
            reversedBroker!!.safe(attempts).reversed(altPrice)
        }
    }
}) {
    val amounts = portfolio.amountsOf(assets.all).toDouble()
    val spreads = history.last().withMainAsset()
    val asks = spreads.map { it.ask }
    val bids = spreads.map { it.bid }
    val capitals = amounts * bids
    val currentPortions = capitals.portions()
    val currentIndex = currentPortions.indexOfMax()
    val currentAsset = assets.all[currentIndex]

    val bestPortions = network.bestPortfolio(currentPortions.withoutMainAsset(), history)
    val buyIndex = bestPortions.indexOfMax()
    val buyAsset = assets.all[buyIndex]

    val tradeAmount = capitals[currentIndex].toBigDecimal()
    if (currentAsset != assets.main) {
        val currentPrice = bids[currentIndex].toBigDecimal()
        sell(currentAsset, currentPrice, tradeAmount)
    }
    if (currentAsset != assets.main) {
        val buyPrice = asks[buyIndex].toBigDecimal()
        buy(buyAsset, buyPrice, tradeAmount)
    }
}

data class TradeResult(private val assetCapitals: Map<Asset, Double>, val totalCapital: Double, private val mainAsset: Asset) {
    override fun toString(): String {
        val totalCapital = "%.4f".format(totalCapital)
        val assetCapitals = assetCapitals.toList().joinToString(", ") {
            val asset = it.first
            val capital = "%.4f".format(it.second)
            "$asset=$capital"
        }
        return "$totalCapital $mainAsset ($assetCapitals)"
    }
}

typealias Capitals = List<Double>
typealias Profits = List<Double>

fun Collection<TradeResult>.capitals(): Capitals = map(TradeResult::totalCapital)
fun Capitals.profits(): Profits = zipWithNext { c, n -> n / c }

fun Profits.daily(period: Duration): Profits {
    val periodsPerDay = (MILLIS_PER_DAY / period.toMillis()).toInt()
    return chunked(periodsPerDay).map {
        product(it).pow(periodsPerDay.toDouble() / it.size)
    }
}

fun Profits.hourly(period: Duration): Profits {
    val periodsPerHour = (MILLIS_PER_HOUR / period.toMillis()).toInt()
    return chunked(periodsPerHour).map {
        product(it).pow(periodsPerHour.toDouble() / it.size)
    }
}

private fun Spreads.withMainAsset(): Spreads = listOf(Spread(1.0, 1.0)) + this
private fun Portions.withoutMainAsset(): Portions = drop(1)