package com.dmi.cointrader.app.neural

import jep.Jep
import java.nio.file.Paths

fun jep() = Jep(false, Paths.get("com/dmi/cointrader/app/python/src").toAbsolutePath().toString()).apply {
    try {
        eval("import sys")
        eval("sys.argv=[''] ")
    } catch (e: Throwable) {
        close()
        throw e
    }
}