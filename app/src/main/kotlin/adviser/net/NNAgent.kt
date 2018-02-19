package adviser.net

import jep.Jep
import jep.NDArray
import util.math.DoubleMatrix2D
import util.math.DoubleMatrix4D
import java.math.BigDecimal
import java.nio.file.Path

class NNAgent(
        private val jep: Jep,
        private val indicatorNumber: Int,
        private val coinNumber: Int,
        private val windowSize: Int,
        private val fee: BigDecimal,
        private val learningRate: BigDecimal,
        netPath: String? = null
): AutoCloseable {
    init {
        jep.eval("from cointrader.util.nnagent import NNAgent")
        jep.eval("from cointrader.util.nnagent import NNConfig")
        jep.eval("agent = None")
        jep.eval("""
                def createAgent(indicator_number, coin_number, window_size, fee, learning_rate, net_path):
                    global agent
                    config = NNConfig(indicator_number=indicator_number, coin_number=coin_number, window_size=window_size, fee=fee, learning_rate=learning_rate)
                    agent = NNAgent(config, net_path)
            """.trimIndent())
        jep.eval("""
                def best_portfolio(history, previous_w):
                    return agent.best_portfolio(history, previous_w)
            """.trimIndent())
        jep.invoke("createAgent", indicatorNumber, coinNumber, windowSize, fee.toDouble(), learningRate.toDouble(), netPath)
    }

    @Suppress("UNCHECKED_CAST")
    fun bestPortfolioPortions(currentPortions: DoubleMatrix2D, history: DoubleMatrix4D): DoubleMatrix2D = synchronized(this) {
        require(currentPortions.n2 == coinNumber)
        require(history.n2 == indicatorNumber)
        require(history.n3 == coinNumber)
        require(history.n4 == windowSize)
        require(currentPortions.n1 == history.n1)

        val nphistory = NDArray(history.data, history.n1, history.n2, history.n3, history.n4)
        val npportions = NDArray(currentPortions.data, currentPortions.n1, currentPortions.n2)

        val result = jep.invoke("best_portfolio", nphistory, npportions) as NDArray<FloatArray>

        val dataDouble = result.data.map { it.toDouble() }.toDoubleArray()
        return DoubleMatrix2D(result.dimensions[0], result.dimensions[1], dataDouble)
    }

    @Suppress("UNCHECKED_CAST")
    fun train(currentPortions: DoubleMatrix2D, history: DoubleMatrix4D, priceIncs: DoubleMatrix2D): TrainResult = synchronized(this) {
        require(currentPortions.n2 == coinNumber)
        require(history.n2 == indicatorNumber)
        require(history.n3 == coinNumber)
        require(history.n4 == windowSize)
        require(priceIncs.n2 == coinNumber)
        require(priceIncs.n1 == currentPortions.n1)
        require(priceIncs.n1 == history.n1)

        val nphistory = NDArray(history.data, history.n1, history.n2, history.n3, history.n4)
        val npportions = NDArray(currentPortions.data, currentPortions.n1, currentPortions.n2)
        val npPriceIncs = NDArray(priceIncs.data, priceIncs.n1, priceIncs.n2)

        val result = jep.invoke("train", nphistory, npportions, npPriceIncs) as Array<*>
        val newPortions = result[0] as NDArray<FloatArray>
        val geometricMeanProfit = result[1] as Double

        val portionsDataDouble = newPortions.data.map { it.toDouble() }.toDoubleArray()

        return TrainResult(
                DoubleMatrix2D(newPortions.dimensions[0], newPortions.dimensions[1], portionsDataDouble),
                geometricMeanProfit
        )
    }

    fun save(path: Path) {
        val pathStr = path.toAbsolutePath().toString()
        jep.eval("agent.save($pathStr)")
    }

    override fun close() {
        jep.eval("agent.recycle()")
    }

    data class TrainResult(val newPortions: DoubleMatrix2D, val geometricMeanProfit: Double)
}