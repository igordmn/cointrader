package exchange.binance.market

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.domain.general.ExchangeInfo
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun loadExchangeInfo(client: BinanceApiAsyncRestClient): ExchangeInfo = suspendCoroutine { continuation ->
    client.getExchangeInfo { result ->
        continuation.resume(result)
    }
}