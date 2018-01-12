package adviser.net

import org.jpy.PyModule
import util.math.DoubleMatrix2D
import util.math.DoubleMatrix4D
import util.python.PythonUtils

class NNAgent(
        fee: Double,
        private val indicatorNumber: Int,
        private val coinNumber: Int,
        private val windowSize: Int,
        restore_dir: String? = null
) {
    private val agentModule = PyModule.importModule("pgportfolio.agent.nnagent")
    private val numpy = PyModule.importModule("numpy")
    private val agent = agentModule.callMethod("NNAgent", fee, indicatorNumber, coinNumber, windowSize, restore_dir)

    fun bestPortfolioPortions(currentPortions: DoubleMatrix2D, history: DoubleMatrix4D): DoubleMatrix2D {
        require(history.n2 == indicatorNumber)
        require(history.n3 == coinNumber)
        require(history.n4 == windowSize)

        val nparray = numpy.callMethod("array", history.data)
                .callMethod("reshape", intArrayOf(history.n1, history.n2, history.n3, history.n4))
        val result = agent.callMethod("best_portfolio", nparray).callMethod("flatten").callMethod("tolist")
        val data = PythonUtils.getDoubleArrayValue(result)
        return DoubleMatrix2D(data, history.n1, coinNumber + 1)
    }
}