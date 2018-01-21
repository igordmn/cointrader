package exchange.binance.market

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import exchange.MarketBroker
import exchange.binance.api.BinanceAPI
import exchange.binance.api.model.NewOrderResponse
import org.slf4j.Logger
import util.math.sum
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
        val slippage = buySlippage(amount, result)
        log.debug("Buy $amount $name:\n$result\nslippage $slippage")
    }

    override suspend fun sell(amount: BigDecimal) {
        // todo брать время с сервера
        val result = api.newOrder(name, OrderSide.SELL, OrderType.MARKET, null, amount.toString(), null, null, null, 5000, Instant.now().toEpochMilli())
        val slippage = sellSlippage(amount, result)
        log.debug("Sell $amount $name:\n$result\nslippage $slippage")
    }

    private fun buySlippage(amount: BigDecimal, result: NewOrderResponse): BigDecimal {
        val desiredSellingAmount = BigDecimal(result.fills.first().price) * amount
        val factSellingAmount = result.fills.map { BigDecimal(it.qty) * BigDecimal(it.price) }.sum()
        return factSellingAmount.divide(desiredSellingAmount, 30).setScale(8)
    }

    private fun sellSlippage(amount: BigDecimal, result: NewOrderResponse): BigDecimal {
        val desiredBuyingAmount = BigDecimal(result.fills.first().price) * amount
        val factBuyingAmount = result.fills.map { BigDecimal(it.qty) * BigDecimal(it.price) }.sum()
        return desiredBuyingAmount.divide(factBuyingAmount, 30).setScale(8)
    }
}