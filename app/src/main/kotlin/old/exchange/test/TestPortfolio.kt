package old.exchange.test

import old.exchange.Portfolio
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

@Suppress("RedundantSuspendModifier")
class TestPortfolio(initialAmounts: Map<String, BigDecimal>) : Portfolio {
    private val threadContext = newSingleThreadContext("testPortfolio")
    private val amounts = HashMap<String, BigDecimal>(initialAmounts)

    override suspend fun amounts(): Map<String, BigDecimal> = suspendCoroutine { continuation ->
        launch(threadContext) {
            synchronized(amounts) {
                continuation.resume(HashMap(amounts))
            }
        }
    }

    suspend fun modify(perform: (Modifier) -> Unit) = suspendCoroutine<Unit> { continuation ->
        launch(threadContext) {
            var throwed: Throwable? = null
            synchronized(amounts) {
                try {
                    perform(Modifier())
                } catch (e: Throwable) {
                    throwed = e
                }
            }
            if (throwed != null) {
                continuation.resumeWithException(throwed!!)
            } else {
                continuation.resume(Unit)
            }
        }
    }

    inner class Modifier {
        operator fun set(coin: String, amount: BigDecimal) {
            require(amount >= BigDecimal.ZERO)
            amounts[coin] = amount
        }

        operator fun get(coin: String): BigDecimal = amounts[coin] ?: BigDecimal.ZERO
    }
}