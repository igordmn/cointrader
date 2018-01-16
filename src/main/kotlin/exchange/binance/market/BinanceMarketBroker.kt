package exchange.binance.market

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.constant.BinanceApiConstants
import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.NewOrder
import exchange.MarketBroker
import exchange.binance.api.BinanceAPI
import exchange.binance.api.DEFAULT_RECEIVING_WINDOW
import org.slf4j.Logger
import trader.AdvisableTrade
import java.math.BigDecimal
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceMarketBroker(
        private val name: String,
        private val api: BinanceAPI
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) {
        api.newOrder(name, OrderSide.BUY, OrderType.MARKET, TimeInForce.IOC, amount.toString(), null, null, null, DEFAULT_RECEIVING_WINDOW, null)
    }

    override suspend fun sell(amount: BigDecimal) {
        api.newOrder(name, OrderSide.SELL, OrderType.MARKET, TimeInForce.IOC, amount.toString(), null, null, null, DEFAULT_RECEIVING_WINDOW, null)
    }
}