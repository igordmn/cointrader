package com.dmi.cointrader.trade

import com.dmi.cointrader.archive.Spread
import com.dmi.cointrader.archive.Spreads
import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.binance.Portfolio
import com.dmi.cointrader.binance.amountsOf
import com.dmi.cointrader.broker.Broker
import com.dmi.cointrader.broker.SafeBroker
import com.dmi.cointrader.broker.reversed
import com.dmi.cointrader.broker.safe
import com.dmi.cointrader.neural.NeuralHistory
import com.dmi.cointrader.neural.NeuralNetwork
import com.dmi.util.lang.indexOfMax
import com.dmi.util.math.portions
import com.dmi.util.math.times
import com.dmi.util.math.toDouble
import java.math.BigDecimal

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

    val bestPortions = network.bestPortfolio(currentPortions, history)
    val buyIndex = bestPortions.indexOfMax()
    val buyAsset = assets.all[buyIndex]

    val tradeAmount = capitals[currentIndex].toBigDecimal()
    if (currentAsset != assets.main) {
        val currentPrice = bids[currentIndex].toBigDecimal()
        sell(currentAsset, currentPrice, tradeAmount)
    }
    if (buyAsset != assets.main) {
        val buyPrice = asks[buyIndex].toBigDecimal()
        buy(buyAsset, buyPrice, tradeAmount)
    }
}

private fun Spreads.withMainAsset(): Spreads = listOf(Spread(1.0, 1.0)) + this