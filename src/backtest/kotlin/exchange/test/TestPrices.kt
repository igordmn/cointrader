package exchange.test

import exchange.Prices
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class TestPrices(
        private var current: BigDecimal
): Prices {
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