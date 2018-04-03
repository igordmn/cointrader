package com.dmi.cointrader.trade

import com.dmi.cointrader.archive.Spreads
import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.binance.Portfolio
import com.dmi.cointrader.binance.amountsOf
import com.dmi.cointrader.broker.Broker
import com.dmi.cointrader.broker.SafeBroker
import com.dmi.cointrader.broker.reversed
import com.dmi.cointrader.broker.safe
import com.dmi.cointrader.TradeAssets
import com.dmi.cointrader.neural.NeuralHistory
import com.dmi.cointrader.neural.NeuralNetwork
import com.dmi.cointrader.neural.Portions
import com.dmi.util.lang.indexOfMax
import com.dmi.util.math.portions
import com.dmi.util.math.times
import com.dmi.util.math.toDouble

suspend fun performTrade(
        assets: TradeAssets,
        portfolio: Portfolio,
        history: NeuralHistory,
        network: NeuralNetwork,
        getBroker: (baseAsset: Asset, quoteAsset: Asset) -> Broker?
) = performTrade(assets, portfolio, history.last(), { network.bestPortfolio(it, history) }, getBroker)

suspend fun performTrade(
        assets: TradeAssets,
        portfolio: Portfolio,
        spreads: Spreads,
        getBestPortions: (current: Portions) -> Portions,
        getBroker: (baseAsset: Asset, quoteAsset: Asset) -> Broker?
) {
    fun brokerFor(altAsset: Asset, altPrice: Double): Broker {
        val attempts = SafeBroker.Attempts(count = 10, amountMultiplier = 0.99)
        val normalBroker = getBroker(assets.main, altAsset)
        val reversedBroker = getBroker(altAsset, assets.main)

        return if (reversedBroker != null) {
            reversedBroker.safe(attempts)
        } else {
            normalBroker!!.safe(attempts).reversed(altPrice.toBigDecimal())
        }
    }

    val amounts = portfolio.amountsOf(assets.all)
    val asks = spreads.map { it.ask }.withMainAsset()
    val bids = spreads.map { it.bid }.withMainAsset()
    val capitals = amounts.toDouble() * bids

    val currentPortions = capitals.portions()
    val currentIndex = currentPortions.indexOfMax()
    val currentAsset = assets.all[currentIndex]

    val bestPortions = getBestPortions(currentPortions)
    val buyIndex = bestPortions.indexOfMax()
    val buyAsset = assets.all[buyIndex]

    if (currentAsset != buyAsset) {
        val sellPrice = bids[currentIndex]
        val buyPrice = asks[buyIndex]
        val sellAmount = amounts[currentIndex]
        val buyAmount = sellAmount * (sellPrice / buyPrice).toBigDecimal()
        if (currentAsset != assets.main) {
            brokerFor(currentAsset, sellPrice).sell(sellAmount)
        }
        if (buyAsset != assets.main) {
            brokerFor(buyAsset, buyPrice).buy(buyAmount)
        }
    }
}

fun List<Double>.withMainAsset() = listOf(1.0) + this