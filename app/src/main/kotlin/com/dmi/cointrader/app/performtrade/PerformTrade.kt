package com.dmi.cointrader.app.performtrade

import com.dmi.cointrader.app.binance.*
import com.dmi.cointrader.app.broker.*
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.history.Archive
import com.dmi.cointrader.app.history.History
import com.dmi.cointrader.app.moment.prices
import com.dmi.cointrader.app.neural.NeuralNetwork
import com.dmi.cointrader.app.test.TestExchange
import com.dmi.util.lang.indexOfMax
import com.dmi.util.math.portions
import com.dmi.util.math.sum
import com.dmi.util.math.times
import com.dmi.util.math.toDouble
import org.slf4j.Logger
import java.awt.Toolkit
import java.math.BigDecimal
import java.time.Clock

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
        archive.load(clock.instant())
        val history = archive.historyAt(period, config.historyCount)
        performTrade(config.assets, network, exchange.portfolio(clock), history, ::broker)
        val info = binanceInfo(config.assets, exchange, clock)
        log.info(info.toString())
    } catch (e: Exception) {
        log.error("exception", e)
        Toolkit.getDefaultToolkit().beep()
    }
}

private suspend fun binanceInfo(assets: TradeAssets, exchange: BinanceExchange, clock: Clock): TradeInfo {
    val infoAsset = "BTC"
    val minBtc = 0.0001
    val portfolio = exchange.portfolio(clock)
    val btcPrices = exchange.btcPrices()
    val assetCapitals = assets.all
            .associate {
                val capital = if (it == infoAsset) {
                    portfolio[it]!!
                } else {
                    portfolio[it]!! * btcPrices[it]!!
                }
                it to capital.toDouble()
            }
            .filter { it.value > minBtc }
    val totalCapital = assetCapitals.values.sum()
    return TradeInfo(assetCapitals, totalCapital, infoAsset)
}

data class TradeInfo(private val assetCapitals: Map<Asset, Double>, val totalCapital: Double, private val mainAsset: Asset) {
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

fun Collection<TradeInfo>.capitals() = map(TradeInfo::totalCapital)

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
    val capitals = amounts * prices
    val currentPortions = capitals.portions()
    val currentIndex = currentPortions.indexOfMax()
    val currentAsset = assets.all[currentIndex]

    val bestPortions = network.bestPortfolio(currentPortions, history)
    val buyIndex = bestPortions.indexOfMax()
    val buyAsset = assets.all[buyIndex]

    val tradeAmount = capitals[currentIndex].toBigDecimal()
    if (currentAsset != assets.main) {
        val currentPrice = prices[currentIndex].toBigDecimal()
        sell(currentAsset, currentPrice, tradeAmount)
    }
    if (currentAsset != assets.main) {
        val buyPrice = prices[buyIndex].toBigDecimal()
        buy(buyAsset, buyPrice, tradeAmount)
    }
}