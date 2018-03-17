package com.dmi.cointrader.app.performtrade

import com.dmi.cointrader.app.binance.Asset
import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.binance.Portfolio
import com.dmi.cointrader.app.binance.amountsOf
import com.dmi.cointrader.app.broker.Broker
import com.dmi.cointrader.app.broker.SafeBroker
import com.dmi.cointrader.app.broker.reversed
import com.dmi.cointrader.app.broker.safe
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.history.Archive
import com.dmi.cointrader.app.history.History
import com.dmi.cointrader.app.moment.prices
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.Portions
import com.dmi.cointrader.app.test.TestExchange
import com.dmi.cointrader.main.TradeConfig
import com.dmi.util.lang.indexOfMax
import com.dmi.util.log.logger
import com.dmi.util.math.portions
import com.dmi.util.math.times
import com.dmi.util.math.toDouble
import java.awt.Toolkit
import java.math.BigDecimal
import java.time.Clock

suspend fun performRealTrade(
        config: TradeConfig,
        exchange: BinanceExchange,
        archive: Archive,
        period: Period,
        clock: Clock,
        network: NeuralNetwork
) {
    val log = logger("realTrades")
    try {
        archive.load(clock.instant())
        object : PerformTradeContext {
            override val mainAsset = config.mainAsset
            override val altAssets = config.altAssets

            suspend override fun history() = archive.historyAt(period, config.historyCount)
            suspend override fun portfolio() = exchange.portfolio(clock.instant())
            suspend override fun bestPortfolio(current: Portions, history: History) = network.bestPortfolio(current, history)
            override fun broker(baseAsset: Asset, quoteAsset: Asset) = exchange.market(baseAsset, quoteAsset)?.broker(clock)
        }.performTrade()
    } catch (e: Exception) {
        log.error("exception", e)
        Toolkit.getDefaultToolkit().beep()
    }
}

suspend fun performTestTrade(
        config: TradeConfig,
        exchange: TestExchange,
        history: History,
        network: NeuralNetwork
) {
    object : PerformTradeContext {
        override val mainAsset = config.mainAsset
        override val altAssets = config.altAssets

        suspend override fun history() = history
        suspend override fun portfolio() = exchange.portfolio()
        suspend override fun bestPortfolio(current: Portions, history: History) = network.bestPortfolio(current, history)
        override fun broker(baseAsset: Asset, quoteAsset: Asset) = exchange.broker(baseAsset, quoteAsset)
    }.performTrade()
}

interface PerformTradeContext {
    val mainAsset: Asset
    val altAssets: List<Asset>
    suspend fun history(): History
    suspend fun portfolio(): Portfolio
    suspend fun bestPortfolio(current: Portions, history: History): Portions
    fun broker(baseAsset: Asset, quoteAsset: Asset): Broker?
}

suspend fun PerformTradeContext.performTrade() = with(object {
    suspend fun sell(altAsset: Asset, altPrice: BigDecimal, mainAmount: BigDecimal) {
        brokerFrom(altAsset, altPrice).buy(mainAmount)
    }

    suspend fun buy(altAsset: Asset, altPrice: BigDecimal, mainAmount: BigDecimal) {
        brokerFrom(altAsset, altPrice).sell(mainAmount)
    }

    fun brokerFrom(altAsset: Asset, altPrice: BigDecimal): Broker {
        val attempts = SafeBroker.Attempts(count = 10, amountMultiplier = 0.99)
        val normalBroker = broker(mainAsset, altAsset)
        val reversedBroker = broker(altAsset, mainAsset)

        return if (normalBroker != null) {
            normalBroker.safe(attempts)
        } else {
            reversedBroker!!.safe(attempts).reversed(altPrice)
        }
    }
}) {
    val allAssets = listOf(mainAsset) + altAssets
    val amounts = portfolio()
            .amountsOf(allAssets)
            .toDouble()
    val history = history()
    val prices = history.last().prices()
    val mainAmounts = amounts * prices
    val currentPortions = mainAmounts.portions()
    val currentIndex = currentPortions.indexOfMax()
    val currentAsset = allAssets[currentIndex]

    val bestPortions = bestPortfolio(currentPortions, history)
    val buyIndex = bestPortions.indexOfMax()
    val buyAsset = allAssets[buyIndex]

    val tradeAmount = mainAmounts[currentIndex].toBigDecimal()
    if (currentAsset != mainAsset) {
        val currentPrice = prices[currentIndex].toBigDecimal()
        sell(currentAsset, currentPrice, tradeAmount)
    }
    if (currentAsset != mainAsset) {
        val buyPrice = prices[buyIndex].toBigDecimal()
        buy(buyAsset, buyPrice, tradeAmount)
    }
}