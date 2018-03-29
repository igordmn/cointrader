package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.binance.BinanceExchange
import com.dmi.cointrader.binance.amountsOf
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.MILLIS_PER_HOUR
import com.dmi.util.math.product
import com.dmi.util.math.times
import com.dmi.util.math.toDouble
import java.time.Clock
import java.time.Duration
import kotlin.math.pow

private val resultAsset = "BTC"
private const val resultMin = 0.0001
private val resultFormat = "%.4f"

suspend fun realTradeResult(assets: TradeAssets, exchange: BinanceExchange, clock: Clock): TradeResult {
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
            .filter { it.value > resultMin }
    val totalCapital = assetCapitals.values.sum()
    return TradeResult(assetCapitals, totalCapital, resultAsset)
}

fun testTradeResult(assets: TradeAssets, exchange: TestExchange, bids: List<Double>): TradeResult {
    val portfolio = exchange.portfolio()
    val amounts = portfolio.amountsOf(assets.all).toDouble()
    val capitals = bids * amounts
    val assetCapitals = assets.all.withIndex().associate {
        it.value to capitals[it.index]
    }.filter {
        it.value > resultMin
    }
    val totalCapital = capitals.sum()
    return TradeResult(assetCapitals, totalCapital, resultAsset)
}

data class TradeResult(private val assetCapitals: Map<Asset, Double>, val totalCapital: Double, private val mainAsset: Asset) {
    override fun toString(): String {
        val totalCapital = resultFormat.format(totalCapital)
        val assetCapitals = assetCapitals.toList().joinToString(", ") {
            val asset = it.first
            val capital = resultFormat.format(it.second)
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