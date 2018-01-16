package exchange.binance.market

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.NewOrder
import exchange.MarketBroker
import org.slf4j.Logger
import trader.AdvisableTrade
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceMarketBroker(
        private val name: String,
        private val client: BinanceApiAsyncRestClient
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) = suspendCoroutine<Unit> { continuation ->
        val order = NewOrder(name, OrderSide.BUY, OrderType.MARKET, TimeInForce.IOC, amount.toString())
        client.newOrder(order) {
            continuation.resume(Unit)
        }
    }

    override suspend fun sell(amount: BigDecimal) = suspendCoroutine<Unit> { continuation ->
        val order = NewOrder(name, OrderSide.SELL, OrderType.MARKET, TimeInForce.IOC, amount.toString())
        client.newOrder(order) {
            continuation.resume(Unit)
        }
    }
}