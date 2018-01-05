package net

import org.jpy.PyLib
import org.jpy.PyModule


fun main(args: Array<String>) {
    System.setProperty("jpy.jpyLib", "D:/Development/Projects/cointrader/lib/native/jpy.cp36-win_amd64.pyd")
    System.setProperty("jpy.pythonLib", "E:/Distr/Portable/Dev/Anaconda3/envs/coin_predict/python36.dll")
    System.setProperty("jpy.pythonPrefix", "E:/Distr/Portable/Dev/Anaconda3/envs/coin_predict")
    System.setProperty("jpy.pythonExecutable", "E:/Distr/Portable/Dev/Anaconda3/envs/coin_predict/python.exe")

    PyLib.startPython()

    try {
        PyModule.extendSysPath("D:\\Development\\Projects\\coin_predict", true)
        val agent2 = NNAgent(0.02, 3, 25, 160, "D:/Development/Projects/coin_predict/train_package/netfile")

//        val matrix = DoubleMatrix4D(DoubleArray(1 * 3 * 25 * 160), 1, 3, 25, 160)
//        val matrix = DoubleMatrix4D(DoubleArray(1 * 3 * 25 * 160), 1, 3, 25, 160)
//        matrix.fill { i1, i2, i3, i4 -> 0.0 }

//        val ag = agent2.bestPortfolio(matrix)
//
//        var h = 6
//        h++

    } finally {
        PyLib.stopPython()
    }
}