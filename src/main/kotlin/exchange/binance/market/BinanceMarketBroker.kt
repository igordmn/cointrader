package exchange.binance.market

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import exchange.MarketBroker
import exchange.binance.api.BinanceAPI
import exchange.binance.api.DEFAULT_RECEIVING_WINDOW
import org.slf4j.Logger
import java.math.BigDecimal

class BinanceMarketBroker(
        private val name: String,
        private val api: BinanceAPI,
        private val log: Logger
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) {
        val result = api.newOrder(name, OrderSide.BUY, OrderType.MARKET, TimeInForce.IOC, amount.toString(), null, null, null, DEFAULT_RECEIVING_WINDOW, null)
        log.debug("Buy $amount $name:\n$result")
    }

    override suspend fun sell(amount: BigDecimal) {
        val result = api.newOrder(name, OrderSide.SELL, OrderType.MARKET, TimeInForce.IOC, amount.toString(), null, null, null, DEFAULT_RECEIVING_WINDOW, null)
        log.debug("Sell $amount $name:\n$result")
    }
}