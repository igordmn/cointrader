package exchange.binance.market

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import exchange.MarketBroker
import exchange.binance.api.BinanceAPI
import org.slf4j.Logger
import java.math.BigDecimal
import java.time.Instant

class BinanceMarketBroker(
        private val name: String,
        private val api: BinanceAPI,
        private val log: Logger
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) {
        // todo брать время с сервера
        val result = api.newOrder(name, OrderSide.BUY, OrderType.MARKET, null, amount.toString(), null, null, null, 5000, Instant.now().toEpochMilli())
        log.debug("Buy $amount $name:\n$result")
    }

    override suspend fun sell(amount: BigDecimal) {
        // todo брать время с сервера
        val result = api.newOrder(name, OrderSide.SELL, OrderType.MARKET, null, amount.toString(), null, null, null, 5000, Instant.now().toEpochMilli())
        log.debug("Sell $amount $name:\n$result")
    }
}