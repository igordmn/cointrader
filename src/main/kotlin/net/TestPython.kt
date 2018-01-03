package net

import org.jpy.PyLib
import org.jpy.PyModule




fun main(args: Array<String>) {
    System.setProperty("jpy.jpyLib", "D:/Development/Projects/cointrader/lib/native/jpy.cp35-win_amd64.pyd")
    System.setProperty("jpy.pythonLib", "E:/Distr/Portable/Dev/Anaconda3/envs/py35/python35.dll")
    System.setProperty("jpy.pythonPrefix", "E:/Distr/Portable/Dev/Anaconda3/envs/py35")
    System.setProperty("jpy.pythonExecutable", "E:/Distr/Portable/Dev/Anaconda3/envs/py35/python.exe")
    PyLib.startPython()

    val h= PyLib.isPythonRunning()

    PyLib.stopPython()

    PyModule.extendSysPath("D:/1/PGPortfolio", true)
    val sys = PyModule.importModule("train.py")
    val argv = sys.getAttribute("argv", Array<String>::class.java)
}