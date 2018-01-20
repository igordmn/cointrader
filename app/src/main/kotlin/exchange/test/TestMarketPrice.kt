package exchange.test

import exchange.MarketPrice
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class TestMarketPrice(
        var current: BigDecimal
) : MarketPrice {
    override suspend fun current(): BigDecimal = suspendCoroutine { continuation ->
        launch {
            delay(50)
            continuation.resume(current)
        }
    }
}