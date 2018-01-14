package exchange.test

import exchange.MarketPrices
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class TestMarketPrices(
        private var current: BigDecimal
): MarketPrices {
    override suspend fun current(): BigDecimal = suspendCoroutine{ continuation ->
        launch {
            delay(50)
            continuation.resume(current)
        }
    }

    fun setCurrent(current: BigDecimal) {
        this.current = current
    }
}