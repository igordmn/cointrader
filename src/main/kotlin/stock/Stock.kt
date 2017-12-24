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
private val previousForPredict = 50
private val tradesInSeries = 100
private val testCount = 100
private val fee = 0.0025
private val hiddenLayerSize = 200

fun main(args: Array<String>) {
    val prices = randomStocks()
    val normalizedPrices = prices.let { normalizePrices(pricesToUpDown(it)) }
    val neurons = netNeurons()
    val weights = randomWeights(neurons)
    val net = Network(neurons, weights)

    repeat(10) {
        println(measureTimeMillis {
            val res = testNet(net, normalizedPrices, prices)
        })
    }
}

fun randomStocks() = (0..1000000).map { random.nextDouble() * 1000 }

fun netNeurons() = Network.Neurons(previousForPredict, hiddenLayerSize, hiddenLayerSize, 3)


fun randomSeries(normalizedPrices: List<Double>, prices: List<Double>): TradeSeries {
    require(normalizedPrices.size >= previousForPredict + tradesInSeries - 1)
    val start = random.nextInt(normalizedPrices.size - tradesInSeries - previousForPredict + 1)
    return tradeSeries(start, tradesInSeries, normalizedPrices, prices)
}

fun allSeries(normalizedPrices: List<Double>, prices: List<Double>): TradeSeries {
    require(normalizedPrices.size >= previousForPredict + tradesInSeries - 1)
    return tradeSeries(0, normalizedPrices.size - previousForPredict + 1, normalizedPrices, prices)
}

private fun tradeSeries(start: Int, tradesInSeries: Int, normalizedPrices: List<Double>, prices: List<Double>): TradeSeries {
    val trades = ArrayList<TradeSeries.Trade>()
    for (tradeI in 0 until tradesInSeries) {
        val previousPrices = ArrayList<Double>(previousForPredict)

        for (previousI in 0 until previousForPredict) {
            previousPrices.add(normalizedPrices[start + tradeI + previousI])
        }
        val lastActualPrice = prices[start + tradeI + previousForPredict]

        trades.add(TradeSeries.Trade(previousPrices, lastActualPrice))
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

fun testNet(net: Network, normalizedPrices: List<Double>, prices: List<Double>): Double {
    val results = (0 until testCount).map {
        val series = randomSeries(normalizedPrices, prices)
        val actions = predictActions(net, series)
        tradeResult(series, actions)
    }
    var mul = 1.0
    results.forEach { mul *= it }
    return mul
}

fun testOnAllNet(net: Network, normalizedPrices: List<Double>, prices: List<Double>): Double {
    val series = allSeries(normalizedPrices, prices)
    val actions = predictActions(net, series)
    return tradeResult(series, actions)
}

fun predictActions(net: Network, series: TradeSeries): List<TradeAction> {
    val input = netInput(series)
    val output = output(net, input)
    return toActions(output)
}

fun netInput(series: TradeSeries): Matrix {
    val data = DoubleArray(previousForPredict * series.trades.size)
    var k = 0
    for (r in 0 until series.trades.size) {
        for (c in 0 until previousForPredict) {
            data[k] = series.trades[r].previousPrices[c]
            k++
        }
    }
    return Matrix(series.trades.size, previousForPredict, data)
}

fun toActions(netOutput: Matrix): List<TradeAction> {
    require(netOutput.cols == 3)
    val res = ArrayList<TradeAction>()
    for (r in 0 until netOutput.rows) {
        val buyVal = netOutput.data[r * netOutput.cols + 0]
        val sellVal = netOutput.data[r * netOutput.cols + 1]
        val holdVal = netOutput.data[r * netOutput.cols + 2]
        when {
            buyVal >= sellVal && buyVal >= holdVal -> res.add(TradeAction.BUY)
            sellVal >= buyVal && sellVal >= holdVal -> res.add(TradeAction.SELL)
            holdVal >= sellVal && holdVal >= buyVal -> res.add(TradeAction.HOLD)
            else -> error("HHHHH")
        }
    }
    return res
}

fun tradeResult(series: TradeSeries, actions: List<TradeAction>): Double {
    require(actions.size == series.trades.size)

    val initialDollars = 1.0
    var dollars = initialDollars
    var coins = 0.0

    fun buy(price: Double) {
        coins += dollars / price * (1 - fee)
        dollars = 0.0
    }

    fun sell(price: Double) {
        dollars += coins * price * (1 - fee)
        coins = 0.0
    }

    actions.forEachIndexed { i, action ->
        val price = series.trades[i].lastActualPrice
        when (action) {
            TradeAction.BUY -> buy(price)
            TradeAction.SELL -> sell(price)
            TradeAction.HOLD -> Unit
        }
    }

    if (dollars == 0.0) {
        dollars += coins * series.trades.last().lastActualPrice
    }

    return dollars / initialDollars
}

enum class TradeAction { BUY, HOLD, SELL }

class TradeSeries(val trades: List<Trade>) {
    class Trade(val previousPrices: List<Double>, val lastActualPrice: Double)
}