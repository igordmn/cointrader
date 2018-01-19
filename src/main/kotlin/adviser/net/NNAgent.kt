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

    fun bestPortfolioPortions(currentPortions: DoubleMatrix2D, history: DoubleMatrix4D): DoubleMatrix2D = synchronized(this) {
        require(currentPortions.n2 == altCoinNumber)
        require(history.n2 == indicatorNumber)
        require(history.n3 == altCoinNumber)
        require(history.n4 == windowSize)

        println("00000000000000000000``")

        val callMethod = numpy.callMethod("array", history.data)
        println("111111111111111111111``")
        val nphistory = callMethod
                .callMethod("reshape", intArrayOf(history.n1, history.n2, history.n3, history.n4))
        println("22222222222222222222222``")
        val callMethod1 = numpy.callMethod("array", currentPortions.data)
        println("333333333333333333333333``")
        val npportions = callMethod1
                .callMethod("reshape", intArrayOf(currentPortions.n1, currentPortions.n2))
        println("444444444444444444444444444444444``")
        val callMethod2 = agent.callMethod("best_portfolio", nphistory, npportions)
        println("555555555555555555555555555555555``")
        val callMethod3 = callMethod2.callMethod("flatten")
        println("5555555555555555555555555555555555555555``")
        val result = callMethod3.callMethod("tolist")
        println("66666666666666666666666666666666``")
        val data = PythonUtils.getDoubleArrayValue(result)
        println("77777777777777777777777777777777777777777``")

        return DoubleMatrix2D(history.n1, altCoinNumber + 1, data)
    }
}