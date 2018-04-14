package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.binance.BinanceExchange
import com.dmi.cointrader.binance.amountsOf
import com.dmi.cointrader.TradeAssets
import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.cointrader.info.ChartData
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.MILLIS_PER_HOUR
import com.dmi.util.lang.times
import com.dmi.util.math.*
import java.time.Clock
import java.time.Duration
import kotlin.math.pow

private val additionalAssets = listOf("BNB")
private val resultAsset = "BTC"
private const val resultMin = 0.0001
private val resultFormat = "%.4f"

suspend fun realTradeResult(assets: TradeAssets, exchange: BinanceExchange, clock: Clock): TradeResult {
    val portfolio = exchange.portfolio(clock)
    val btcPrices = exchange.btcPrices()
    val assetCapitals = (assets.all + additionalAssets)
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

data class TradeResult(private val assetCapitals: Map<Asset, Double>, val totalCapital: Capital, private val mainAsset: Asset) {
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

fun tradeSummary(
        space: PeriodSpace,
        tradePeriods: Int,
        capitals: Capitals,
        previous: List<TradeSummary>,
        movingAverageCount: Int = 10
): TradeSummary {
    val tradePeriodsPerDay = space.periodsPerDay() / tradePeriods.toDouble()
    val tradeDuration = space.duration * tradePeriods
    val profits = capitals.profits()
    val dailyProfits = profits.daily(tradeDuration)
    return TradeSummary(
            averageDayProfit = if (previous.size >= movingAverageCount) {
                geoMean(previous.slice(previous.size - movingAverageCount until previous.size).map { it.dayProfitMean })
            } else {
                null
            },
            dayProfitMean = dailyProfits.let(::geoMean),
            dayProfitMedian = dailyProfits.median(),
            downsideDeviation = profits.let(::downsideDeviation),
            maximumDrawdawn = profits.let(::maximumDrawdawn),
            dailyDownsideDeviation = dailyProfits.let(::downsideDeviation),
            dailyMaximumDrawdawn = dailyProfits.let(::maximumDrawdawn),
            chartData = run {
                val profitDays = capitals.indices.map { it / tradePeriodsPerDay }
                ChartData(profitDays.toDoubleArray(), capitals.toDoubleArray())
            }
    )
}

data class TradeSummary(
        val averageDayProfit: Double?,
        val dayProfitMean: Double,
        val dayProfitMedian: Double,
        val downsideDeviation: Double,
        val maximumDrawdawn: Double,
        val dailyDownsideDeviation: Double,
        val dailyMaximumDrawdawn: Double,
        val chartData: ChartData
) {
    override fun toString(): String {
        val dayProfitMean = "%.3f".format(dayProfitMean)
        val dayProfitMedian = "%.3f".format(dayProfitMedian)
        val averageDayProfit = if (averageDayProfit != null) "%.3f".format(averageDayProfit) else "-----"
        val negativeDeviation = "%.5f".format(downsideDeviation)
        val maximumDrawdawn = "%.2f".format(maximumDrawdawn)
        val dailyDownsideDeviation = "%.5f".format(dailyDownsideDeviation)
        val dailyMaximumDrawdawn = "%.2f".format(dailyMaximumDrawdawn)
        return "$averageDayProfit $dayProfitMean $dayProfitMedian $negativeDeviation $maximumDrawdawn $dailyDownsideDeviation $dailyMaximumDrawdawn"
    }
}

typealias Capital = Double
typealias Capitals = List<Capital>
typealias Profits = List<Double>

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