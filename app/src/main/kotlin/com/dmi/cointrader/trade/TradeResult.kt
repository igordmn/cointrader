package com.dmi.cointrader.trade

import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.binance.BinanceExchange
import com.dmi.cointrader.binance.amountsOf
import com.dmi.cointrader.TradeAssets
import com.dmi.cointrader.archive.PeriodSpace
import com.dmi.cointrader.info.ChartData
import com.dmi.cointrader.test.TestExchange
import com.dmi.util.math.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.lang.Math.pow
import java.math.BigDecimal
import java.time.Clock
import kotlin.math.ln

private val additionalAssets = listOf("BNB")
private val resultAsset = "BTC"
private const val resultMin = 0.0001
private val resultFormat = "%.4f"

suspend fun realTradeResult(assets: TradeAssets, exchange: BinanceExchange, clock: Clock): TradeResult {
    val portfolio = exchange.portfolio(clock)
    val btcPrices = exchange.btcPrices()
    val assetCapitals = (assets.all + additionalAssets)
            .associate {
                it to portfolio[it]!!
            }
            .filter { it.value > BigDecimal.ZERO }
            .mapValues {
                val capital = if (it.key == resultAsset) {
                    it.value
                } else {
                    it.value * btcPrices[it.key]!!
                }
                capital.toDouble()
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

data class TradeResult(val assetCapitals: Map<Asset, Double>, val totalCapital: Capital, val mainAsset: Asset) {
    override fun toString(): String {
        val totalCapital = resultFormat.format(totalCapital)
        val assetCapitals = assetCapitals.toList().joinToString(", ") {
            val asset = it.first
            val capital = resultFormat.format(it.second)
            "$asset=$capital"
        }
        return "$totalCapital ($assetCapitals)"
    }
}

fun List<TradeResult>.divideByFirstCapital() = let {
    val firstCapital = it[0].totalCapital
    it.map {
        TradeResult(
                it.assetCapitals.mapValues { it.value / firstCapital },
                it.totalCapital / firstCapital,
                it.mainAsset
        )
    }
}

fun tradeSummary(
        space: PeriodSpace,
        tradePeriods: Int,
        capitals: Capitals,
        previous: List<TradeSummary>,
        movingAverageCount: Int = 10,
        averageWindowSize: Int = 16
): TradeSummary {
    val tradePeriodsPerDay = space.periodsPerDay() / tradePeriods.toDouble()
    val profits = capitals.profits()
    return TradeSummary(
            profits = profits,
            averageDayProfit = if (previous.size >= movingAverageCount) {
                previous.slice(previous.size - movingAverageCount until previous.size).map { it.dayProfit }.geoMean()
            } else {
                null
            },
            dayProfit = pow(profits.geoMean(), tradePeriodsPerDay),
            score1 = profits.map(::ln).sharpeRatio(),
            score2 = profits.windowed(averageWindowSize) { it.geoMean() }
                    .sortAndRemoveOutliers(percent = 0.25)
                    .geoMean()
                    .let { pow(it, tradePeriodsPerDay) },
            chartData = run {
                val profitDays = capitals.indices.map { it / tradePeriodsPerDay }
                ChartData(profitDays.toDoubleArray(), capitals.toDoubleArray())
            },
            chartData2 = run {
                var acc = 1.0
                val capitals2 = profits
                        .windowed(averageWindowSize, transform = List<Double>::geoMean)
                        .limitOutliers(percent = 0.25)
                        .map {
                            val old = acc
                            acc *= it
                            old
                        }
                val profitDays = capitals2.indices.map { it / tradePeriodsPerDay }
                ChartData(profitDays.toDoubleArray(), capitals2.toDoubleArray())
            }
    )
}

@Serializable
data class TradeSummary(
        val profits: List<Double>,
        val averageDayProfit: Double?,
        val dayProfit: Double,
        val score1: Double,
        val score2: Double,
        @Transient val chartData: ChartData = ChartData(DoubleArray(0), DoubleArray(0)),
        @Transient val chartData2: ChartData = ChartData(DoubleArray(0), DoubleArray(0))
) {
    override fun toString(): String {
        val averageDayProfit = "%.3f".format(averageDayProfit)
        val dayProfit = "%.3f".format(dayProfit)
        val score1 = "%.3f".format(score1)
        val score2 = "%.3f".format(score2)
        return "$averageDayProfit $dayProfit $score1 $score2"
    }
}

typealias Capital = Double
typealias Capitals = List<Capital>
typealias Profits = List<Double>

fun Capitals.profits(): Profits = zipWithNext { c, n -> n / c }