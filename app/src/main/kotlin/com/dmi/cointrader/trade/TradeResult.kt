package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.Asset
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.MILLIS_PER_HOUR
import com.dmi.util.math.product
import java.time.Duration
import kotlin.math.pow

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