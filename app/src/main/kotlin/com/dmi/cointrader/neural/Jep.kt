package com.dmi.cointrader.neural

import com.dmi.util.io.ResourceContext
import jep.Jep
import jep.JepConfig
import jep.NDArray
import java.nio.file.Paths

typealias NDDoubleArray = NDArray<DoubleArray>
typealias NDFloatArray = NDArray<FloatArray>

fun ResourceContext.jep(): Jep = Jep(
        JepConfig()
                .setIncludePath(Paths.get("python").toAbsolutePath().toString())
                .setRedirectOutputStreams(true)
).apply {
    try {
        eval("import sys")
        eval("sys.argv=[''] ")
    } catch (e: Throwable) {
        close()
        throw e
    }
}.use()