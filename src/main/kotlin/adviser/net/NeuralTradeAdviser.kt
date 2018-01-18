package adviser.net

import adviser.AdviseIndicators
import adviser.CoinPortions
import adviser.TradeAdviser
import exchange.candle.Candle
import util.math.DoubleMatrix4D
import exchange.candle.CoinToCandles
import util.lang.unsupportedOperation
import util.math.DoubleMatrix2D
import util.math.portions
import java.math.BigDecimal
import java.nio.file.Path

class NeuralTradeAdviser(
        private val mainCoin: String,
        private val altCoins: List<String>,
        private val previousCount: Int,
        private val netDirectory: Path,
        fee: BigDecimal,
        private val indicators: AdviseIndicators

) : TradeAdviser {
    private val coinCount = 1 + altCoins.size
    private val net = NNAgent(fee.toDouble(), indicators.count, altCoins.size, previousCount, netDirectory.toAbsolutePath().toString())

    override suspend fun bestPortfolioPortions(currentPortions: CoinPortions, previousCandles: CoinToCandles): CoinPortions {
        val scale = currentPortions[mainCoin]!!.scale()
        return net.bestPortfolioPortions(
                currentPortions.toMatrix(),
                previousCandles.toMatrix()
        ).toPortions(scale)
    }

    private fun CoinPortions.toMatrix() = DoubleMatrix2D(1, coinCount) { _, index ->
        val coin = coinOf(index)
        this[coin]!!.toDouble()
    }

    private fun DoubleMatrix2D.toPortions(scale: Int): CoinPortions {
        val portions = HashMap<String, BigDecimal>()
        forEach { _, coinIndex, value ->
            val coin = coinOf(coinIndex)
            portions[coin] = BigDecimal(value)
        }
        return portions.portions(scale)
    }

    private fun CoinToCandles.toMatrix() = DoubleMatrix4D(
            1, indicators.count, altCoins.size, previousCount
    ) { _, indicatorIndex, altCoinIndex, candleIndex ->
        val coin = altCoins[altCoinIndex]
        val candles = this[coin]!!
        val candle = candles[candleIndex]
        candle.indicatorValue(indicatorIndex)
    }

    private fun Candle.indicatorValue(index: Int): Double = when (index) {
        0 -> close.toDouble()
        1 -> low.toDouble()
        2 -> high.toDouble()
        else -> unsupportedOperation()
    }

    private fun coinOf(index: Int): String = if (index == 0) mainCoin else altCoins[index - 1]
}