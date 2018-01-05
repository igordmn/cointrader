package net

import org.jpy.PyModule

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

    fun gg(obj: DoubleMatrix4D): DoubleMatrix4D {
        val nmarray = numpy.callMethod("array", obj.data)
                .callMethod("reshape", intArrayOf(obj.n1, obj.n2, obj.n3, obj.n4))
        val result = agent.callMethod("gg", nmarray).callMethod("flatten")
        return DoubleMatrix4D(PythonUtils.getDoubleArrayValue(result), obj.n1, obj.n2, obj.n3, obj.n4)
    }
}