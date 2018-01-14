package adviser.net

import org.jpy.PyModule
import util.math.DoubleMatrix2D
import util.math.DoubleMatrix4D
import util.python.PythonUtils

class NNAgent(
        fee: Double,
        private val indicatorNumber: Int,
        private val altCoinNumber: Int,
        private val windowSize: Int,
        restore_dir: String? = null
) {
    private val agentModule = PyModule.importModule("pgportfolio.agent.nnagent")
    private val numpy = PyModule.importModule("numpy")
    private val agent = agentModule.callMethod("NNAgent", fee, indicatorNumber, altCoinNumber, windowSize, restore_dir)

    // TODO
    fun bestPortfolioPortions(currentPortions: DoubleMatrix2D, history: DoubleMatrix4D): DoubleMatrix2D {
        require(currentPortions.n2 == altCoinNumber + 1)  // with main coin
        require(history.n2 == indicatorNumber)
        require(history.n3 == altCoinNumber)
        require(history.n4 == windowSize)

        val nphistory = numpy.callMethod("array", history.data)
                .callMethod("reshape", intArrayOf(history.n1, history.n2, history.n3, history.n4))
        val npportions = numpy.callMethod("array", currentPortions.data)
                .callMethod("reshape", intArrayOf(currentPortions.n1, currentPortions.n2))

        val result = agent.callMethod("best_portfolio", nphistory, npportions).callMethod("flatten").callMethod("tolist")
        val data = PythonUtils.getDoubleArrayValue(result)

        return DoubleMatrix2D(history.n1, altCoinNumber + 1, data)
    }
}