package adviser.net

import jep.Jep
import jep.NDArray
import util.math.DoubleMatrix2D
import util.math.DoubleMatrix4D

class NNAgent(
        private val jep: Jep,
        fee: Double,
        private val indicatorNumber: Int,
        private val altCoinNumber: Int,
        private val windowSize: Int,
        netPath: String? = null
) {
    init {
        jep.eval("from cointrader.util.nnagent import NNAgent")
        jep.eval("agent = None")
        jep.eval("""
                def createAgent(fee, indicatorNumber, altCoinNumber, windowSize, netPath):
                    global agent
                    agent = NNAgent(fee, indicatorNumber, altCoinNumber, windowSize, netPath)
            """.trimIndent())
        jep.eval("""
                def best_portfolio(history, previous_w):
                    return agent.best_portfolio(history, previous_w)
            """.trimIndent())
        jep.invoke("createAgent", fee, indicatorNumber, altCoinNumber, windowSize, netPath)
    }

    @Suppress("UNCHECKED_CAST")
    fun bestPortfolioPortions(currentPortions: DoubleMatrix2D, history: DoubleMatrix4D): DoubleMatrix2D = synchronized(this) {
        require(currentPortions.n2 == altCoinNumber)
        require(history.n2 == indicatorNumber)
        require(history.n3 == altCoinNumber)
        require(history.n4 == windowSize)

        val nphistory = NDArray(history.data, history.n1, history.n2, history.n3, history.n4)
        val npportions = NDArray(currentPortions.data, currentPortions.n1, currentPortions.n2)

        val result = jep.invoke("best_portfolio", nphistory, npportions) as NDArray<FloatArray>

        val dataDouble = result.data.map { it.toDouble() }.toDoubleArray()
        return DoubleMatrix2D(result.dimensions[0], result.dimensions[1], dataDouble)
    }
}