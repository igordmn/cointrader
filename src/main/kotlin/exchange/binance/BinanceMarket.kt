package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.NewOrder
import exchange.Market
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceMarket(
        private val name: String,
        private val client: BinanceApiAsyncRestClient
) : Market {
    override fun history() = BinanceMarketHistory(name, client)

    override suspend fun buy(amount: BigDecimal) = suspendCoroutine<Unit> { continuation ->
        val order = NewOrder(name, OrderSide.BUY, OrderType.MARKET, TimeInForce.IOC, amount.toString())
        client.newOrder(order) { response ->
            continuation.resume(Unit)
        }
    }

    override suspend fun sell(amount: BigDecimal) = suspendCoroutine<Unit> { continuation ->
        val order = NewOrder(name, OrderSide.SELL, OrderType.MARKET, TimeInForce.IOC, amount.toString())
        client.newOrder(order) { response ->
            continuation.resume(Unit)
        }
    }
}