package com.dmi.cointrader.neural

import jep.Jep
import jep.JepConfig
import jep.NDArray
import java.nio.file.Paths

typealias NDDoubleArray = NDArray<DoubleArray>
typealias NDFloatArray = NDArray<FloatArray>

fun jep() = Jep(
        JepConfig()
                .setIncludePath(Paths.get("python/src").toAbsolutePath().toString())
                .setRedirectOutputStreams(true)
).apply {
    try {
        eval("import sys")
        eval("sys.argv=[''] ")
    } catch (e: Throwable) {
        close()
        throw e
    }
}

fun numpyArray(n1: Int,
               n2: Int,
               n3: Int,
               n4: Int,
               value: (i1: Int, i2: Int, i3: Int, i4: Int) -> Double
): NDDoubleArray {
    val data = DoubleArray(n1 * n2 * n3 * n4)
    var k = 0
    for (i1 in 0 until n1)
        for (i2 in 0 until n2)
            for (i3 in 0 until n3)
                for (i4 in 0 until n4)
                    data[k++] = value(i1, i2, i3, i4)
    return NDArray(data, n1, n2, n3, n4)
}

fun numpyArray(n1: Int,
               n2: Int,
               value: (i1: Int, i2: Int) -> Double
): NDDoubleArray {
    val data = DoubleArray(n1 * n2)
    var k = 0
    for (i1 in 0 until n1)
        for (i2 in 0 until n2)
            data[k++] = value(i1, i2)
    return NDArray(data, n1, n2)
}

operator fun NDFloatArray.get(i1: Int, i2: Int) = data[i2 * dimensions[0] + i1]

fun NDFloatArray.forEach(accept: (i1: Int, i2: Int, value: Float) -> Unit) {
    var k = 0
    for (i1 in 0 until dimensions[0])
        for (i2 in 0 until dimensions[1])
            accept(i1, i2, data[k++])
}