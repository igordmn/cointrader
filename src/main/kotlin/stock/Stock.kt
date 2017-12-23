package stock

import matrix.Matrix
import net.Network
import net.output
import net.randomWeights
import java.lang.Math.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.measureTimeMillis
import kotlin.sequences.zipWithNext

private val random = Random(System.nanoTime())
private val previousForPredict = 60
private val tradesInSeries = 80
private val testCount = 20
private val fee = 0.0025

fun main(args: Array<String>) {
    val stocks = randomStocks()
    val neurons = netNeurons()
    val weights = randomWeights(neurons)
    val net = Network(neurons, weights)

    repeat(10) {
        println(measureTimeMillis {
            val res = testNet(net, stocks)
        })
    }
}

fun randomStocks() = (0..1000000)
        .map { random.nextDouble() }
        .let {
            normalizePrices(pricesToUpDown(it))
        }

fun netNeurons() = Network.Neurons(previousForPredict, 512, 512, 1)


fun randomSeries(stocks: List<Double>): TradeSeries {
    require(stocks.size >= previousForPredict + tradesInSeries - 1)
    val start = random.nextInt(stocks.size - tradesInSeries - previousForPredict + 1)
    val trades = ArrayList<TradeSeries.Trade>()
    for (tradeI in 0 until tradesInSeries) {
        val previousPrices = ArrayList<Double>(previousForPredict)

        for (previousI in 0 until previousForPredict) {
            previousPrices.add(stocks[start + tradeI + previousI])
        }

        trades.add(TradeSeries.Trade(previousPrices))
    }
    return TradeSeries(trades)
}

fun pricesToUpDown(prices: List<Double>): List<Double> =
        prices.asSequence()
                .zipWithNext { current, next -> log(next / current) }
                .toList()

fun normalizePrices(prices: List<Double>): List<Double> {
    val max = prices.max()!!
    val min = prices.min()!!
    val absMax = max(abs(max), abs(min))
    fun normalize(value: Double) = value / absMax
    return prices.map(::normalize)
}

fun testNet(net: Network, stocks: List<Double>): Double {
    val results = (0 until testCount).map {
        val series = randomSeries(stocks)
        val actions = predictActions(net, series)
        tradeResult(series, actions)
    }
    return results.average()
}

fun predictActions(net: Network, series: TradeSeries): List<TradeAction> {
    val input = netInput(series)
    val output = output(net, input)
    return toActions(output)
}

fun netInput(series: TradeSeries): Matrix {
    val data = DoubleArray(previousForPredict * tradesInSeries)
    var k = 0
    for (r in 0 until tradesInSeries) {
        for (c in 0 until previousForPredict) {
            data[k] = series.trades[r].previousPrices[c]
            k++
        }
    }
    return Matrix(tradesInSeries, previousForPredict, data)
}

fun toActions(netOutput: Matrix): List<TradeAction> {
    require(netOutput.cols == 1)
    val buyThreshold = 0.75
    val sellThreshold = -0.75
    return netOutput.data.map {
        when {
            it >= buyThreshold -> TradeAction.BUY
            it <= sellThreshold -> TradeAction.SELL
            else -> TradeAction.HOLD
        }
    }
}

fun tradeResult(series: TradeSeries, actions: List<TradeAction>): Double {
    require(actions.size == series.trades.size)

    val initialDollars = 1.0
    var actualPrice = 1.0
    var dollars = initialDollars
    var coins = 0.0

    val prices = series.trades.map { it.previousPrices.last() }

    fun buy(price: Double) {
        coins += dollars / price * (1 - fee)
        dollars = 0.0
    }

    fun sell(price: Double) {
        dollars += coins * price * (1 - fee)
        coins = 0.0
    }

    actions.forEachIndexed { i, action ->
        actualPrice *= Math.exp(prices[i])
        when (action) {
            TradeAction.BUY -> buy(actualPrice)
            TradeAction.SELL -> sell(actualPrice)
            TradeAction.HOLD -> Unit
        }
    }

    if (dollars == 0.0) {
        dollars += coins * actualPrice * (1 - fee)
    }

    return dollars / initialDollars
}

enum class TradeAction { BUY, HOLD, SELL }

class TradeSeries(val trades: List<Trade>) {
    class Trade(val previousPrices: List<Double>)
}