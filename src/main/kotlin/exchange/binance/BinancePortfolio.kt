package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import exchange.Portfolio
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class BinancePortfolio(
        private val client: BinanceApiAsyncRestClient,
        private val info: BinanceInfo
) : Portfolio {
    override suspend fun amounts(): Map<String, BigDecimal> = suspendCoroutine { continuation ->
        client.getAccount { account ->
            val amounts = account.balances.associate {
                val standardName = info.binanceNameToStandard[it.asset] ?: it.asset
                standardName to BigDecimal(it.free)
            }
            continuation.resume(amounts)
        }
    }
}