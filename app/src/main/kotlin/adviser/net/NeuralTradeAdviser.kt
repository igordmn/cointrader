package adviser.net

import adviser.AdviseIndicators
import adviser.CoinPortions
import adviser.TradeAdviser
import exchange.candle.Candle
import util.math.DoubleMatrix4D
import exchange.candle.CoinToCandles
import jep.Jep
import main.test.Config
import util.lang.unsupportedOperation
import util.math.DoubleMatrix2D
import util.math.portions
import java.math.BigDecimal
import java.nio.file.Path
import java.nio.file.Paths

fun neuralTradeAdviser(jep: Jep, operationScale: Int, config: Config) = NeuralTradeAdviser(
        jep,
        operationScale,
        listOf(config.mainCoin) + config.altCoins,
        config.historyCount,
        Paths.get("data/train_package/netfile"),
        config.fee,
        config.learningRate,
        config.indicators
)

class NeuralTradeAdviser(
        jep: Jep,
        private val operationScale: Int,
        private val coins: List<String>,
        private val previousCount: Int,
        private val netPath: Path,
        fee: BigDecimal,
        learningRate: BigDecimal,
        private val indicators: AdviseIndicators

) : TradeAdviser {
    private val net = NNAgent(jep, indicators.count, coins.size, previousCount, fee, learningRate, netPath.toAbsolutePath().toString())

    override suspend fun bestPortfolioPortions(currentPortions: CoinPortions, previousCandles: CoinToCandles): CoinPortions {
        return net.bestPortfolioPortions(
                currentPortions.toMatrix(),
                previousCandles.toMatrix()
        ).toPortions()
    }

    private fun CoinPortions.toMatrix() = DoubleMatrix2D(1, coins.size) { _, index ->
        val coin = coins[index]
        this[coin]!!.toDouble()
    }

    private fun DoubleMatrix2D.toPortions(): CoinPortions {
        val portions = HashMap<String, BigDecimal>()
        forEach { _, coinIndex, value ->
            val coin = coins[coinIndex]
            portions[coin] = BigDecimal(value)
        }
        return portions.portions(operationScale)
    }

    private fun CoinToCandles.toMatrix() = DoubleMatrix4D(
            1, indicators.count, coins.size, previousCount
    ) { _, indicatorIndex, coinIndex, candleIndex ->
        val coin = coins[coinIndex]
        val candles = this[coin]!!.reversed()
        val candle = candles[candleIndex]
        candle.indicatorValue(indicatorIndex)
    }

    private fun Candle.indicatorValue(index: Int): Double = when (index) {
        0 -> close.toDouble()
        1 -> high.toDouble()
        2 -> low.toDouble()
        else -> unsupportedOperation()
    }
}