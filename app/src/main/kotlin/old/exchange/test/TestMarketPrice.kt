package old.exchange.test

import old.exchange.MarketPrice
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class TestMarketPrice(
        var current: BigDecimal
) : MarketPrice {
    override suspend fun current(): BigDecimal = suspendCoroutine { continuation ->
        launch {
            continuation.resume(current)
        }
    }
}