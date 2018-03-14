package com.dmi.cointrader.app.performtrade

import com.dmi.cointrader.app.binance.Asset
import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.binance.Portfolio
import com.dmi.cointrader.app.binance.amountsOf
import com.dmi.cointrader.app.broker.Broker
import com.dmi.cointrader.app.broker.SafeBroker
import com.dmi.cointrader.app.broker.reversed
import com.dmi.cointrader.app.broker.safe
import com.dmi.cointrader.app.history.History
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.neural.Portions
import com.dmi.cointrader.main.TradeConfig
import com.dmi.util.lang.indexOfMax
import com.dmi.util.math.portions
import com.dmi.util.math.times
import com.dmi.util.math.toDouble
import org.slf4j.Logger
import java.awt.Toolkit
import java.math.BigDecimal
import java.time.Clock

suspend fun performRealTrade(
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

interface PerformTradeContext {
    val mainAsset: Asset
    val altAssets: List<Asset>
    suspend fun history(): History.Window
    suspend fun portfolio(): Portfolio
    suspend fun bestPortfolio(current: Portions, history: History.Window): Portions
    fun broker(baseAsset: Asset, toAsset: Asset): Broker?
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
    val prices = history.prices
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