package train

import exchange.ReversedMarketHistory
import exchange.binance.BinanceConstants
import exchange.binance.market.PreloadedBinanceMarketHistories
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import exchange.history.MarketHistory
import exchange.history.NormalizedMarketHistory
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.coroutines.experimental.channels.toList
import util.lang.truncatedTo
import java.time.Duration
import java.time.Instant

class TrainData(
        private val data: FloatArray,
        private val coinCount: Int,
        val size: Int
) {

    init {
        require(data.size == size * coinCount * Candle.indicatorCount)
    }

    operator fun get(index: Int): Moment {
        require(index in 0 until size)
        val start = index * coinCount * Candle.indicatorCount
        var k = start
        val coinIndexToCandle = ArrayList<Candle>(coinCount)
        for (c in 0 until coinCount) {
            val close = data[k++]
            val high = data[k++]
            val low = data[k++]
            val candle = Candle(close, high, low)
            coinIndexToCandle.add(candle)
        }
        return Moment(coinIndexToCandle)
    }

    data class Moment(val coinIndexToCandle: List<Candle>)
    data class Candle(val close: Float, val high: Float, val low: Float) {
        companion object {
            const val indicatorCount = 3 // close, high, low
        }
    }
}

suspend fun loadTrainData(constants: BinanceConstants, mainCoin: String, altCoins: List<String>, histories: PreloadedBinanceMarketHistories, from: Instant, to: Instant): TrainData {
    val operationScale = 32
    val period = Duration.ofMinutes(1)
    val fromTruncated = from.truncatedTo(period)
    val toTruncated = to.truncatedTo(period)
    val diff = Duration.between(fromTruncated, toTruncated)

    val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
    val normalizer = approximateCandleNormalizer(approximatedPricesFactory)

    val candleCount = diff.toMinutes().toInt()
    val coinCount = altCoins.size
    val indicatorCount = TrainData.Candle.indicatorCount
    val data = FloatArray(candleCount * altCoins.size * indicatorCount)

    fun history(altCoin: String): MarketHistory {
        val name = constants.marketName(altCoin, mainCoin)
        val reversedName = constants.marketName(mainCoin, altCoin)

        return when {
            name != null -> histories[name]
            reversedName != null -> ReversedMarketHistory(histories[reversedName], operationScale)
            else -> throw UnsupportedOperationException()
        }
    }

    altCoins.forEachIndexed { coinIndex, coin ->
        val binanceHistory = history(coin)
        val history = NormalizedMarketHistory(binanceHistory, normalizer, period)
        val candles = history.candlesBefore(toTruncated).takeWhile { it.timeRange.start >= fromTruncated }.toList()
        require(candles.size == candleCount)

        candles.forEachIndexed { candleIndex, candle ->
            val start = candleIndex * coinCount * indicatorCount + coinIndex * indicatorCount
            data[start + 0] = candle.item.close.toFloat()
            data[start + 1] = candle.item.high.toFloat()
            data[start + 2] = candle.item.low.toFloat()
        }
    }

    return TrainData(data, coinCount, candleCount)
}