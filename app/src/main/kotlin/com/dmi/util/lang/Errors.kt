package com.dmi.util.lang

import com.dmi.cointrader.trade.performRealTrade
import com.dmi.util.concurrent.delay
import org.slf4j.Logger
import java.time.Clock
import java.time.Duration

fun unsupported(msg: String? = null): Nothing {
    val exception: Throwable = if (msg != null) UnsupportedOperationException(msg) else UnsupportedOperationException()
    throw exception
}

inline fun <T, reified E : Throwable> retry(attempts: Int, block: (attempt: Int) -> T): T {
    var attempt = 0
    while (true) {
        try {
            return block(attempt)
        } catch (e: Throwable) {
            if (e is E) {
                if (attempt == attempts - 1) {
                    throw e
                } else {
                    attempt++
                    continue
                }
            } else {
                throw e
            }
        }
    }
}