package exchange.test

import exchange.Portfolio
import kotlinx.coroutines.experimental.launch
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

@Suppress("RedundantSuspendModifier")
class TestPortfolio(private val initialAmounts: Map<String, BigDecimal>) : Portfolio {
    private val amounts = HashMap<String, BigDecimal>(initialAmounts)

    val coins: Set<String> = amounts.keys

    override suspend fun amounts(): Map<String, BigDecimal> = suspendCoroutine { continuation ->
        launch {
            synchronized(amounts) {
                continuation.resume(HashMap(amounts))
            }
        }
    }

    fun transaction(action: (Modifier) -> Unit) = synchronized(amounts) {
        action(Modifier())
    }

    inner class Modifier {
        operator fun set(coin: String, amount: BigDecimal) {
            amounts[coin] = amount
        }

        operator fun get(coin: String): BigDecimal = amounts[coin]!!
    }
}