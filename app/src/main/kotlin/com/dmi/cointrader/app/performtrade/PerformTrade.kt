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
import com.dmi.cointrader.app.test.TestExchange
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
        fun broker(baseAsset: Asset, quoteAsset: Asset) = exchange.market(baseAsset, quoteAsset)?.broker(clock)
        val history = archive.historyAt(period, config.historyCount)
        performTrade(config.assets, network, exchange.portfolio(clock.instant()), history, ::broker)
    } catch (e: Exception) {
        log.error("exception", e)
        Toolkit.getDefaultToolkit().beep()
    }
}

suspend fun performTestTrade(
        config: TradeConfig,
        exchange: TestExchange,
        archive: Archive,
        period: Period,
        network: NeuralNetwork
) {
    val history = archive.historyAt(period, config.historyCount)
    performTrade(config.assets, network, exchange.portfolio(), history, exchange::broker)
}

data class TradeAssets(val main: Asset, val alts: List<Asset>) {
    val all = listOf(main) + alts
}

suspend fun performTrade(
        assets: TradeAssets,
        network: NeuralNetwork,
        portfolio: Portfolio,
        history: History,
        broker: (baseAsset: Asset, quoteAsset: Asset) -> Broker?
) = with(object {
    suspend fun sell(altAsset: Asset, altPrice: BigDecimal, mainAmount: BigDecimal) {
        brokerFrom(altAsset, altPrice).buy(mainAmount)
    }

    suspend fun buy(altAsset: Asset, altPrice: BigDecimal, mainAmount: BigDecimal) {
        brokerFrom(altAsset, altPrice).sell(mainAmount)
    }

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
    val prices = history.last().prices()
    val mainAmounts = amounts * prices
    val currentPortions = mainAmounts.portions()
    val currentIndex = currentPortions.indexOfMax()
    val currentAsset = assets.all[currentIndex]

    val bestPortions = network.bestPortfolio(currentPortions, history)
    val buyIndex = bestPortions.indexOfMax()
    val buyAsset = assets.all[buyIndex]

    val tradeAmount = mainAmounts[currentIndex].toBigDecimal()
    if (currentAsset != assets.main) {
        val currentPrice = prices[currentIndex].toBigDecimal()
        sell(currentAsset, currentPrice, tradeAmount)
    }
    if (currentAsset != assets.main) {
        val buyPrice = prices[buyIndex].toBigDecimal()
        buy(buyAsset, buyPrice, tradeAmount)
    }
}