package old.exchange.binance.market

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.exception.BinanceApiException
import old.exchange.MarketBroker
import old.exchange.binance.api.BinanceAPI
import old.exchange.binance.api.model.NewOrderResponse
import org.slf4j.Logger
import com.dmi.util.math.sum
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class BinanceMarketBroker(
        private val name: String,
        private val api: BinanceAPI,
        private val log: Logger
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) {
        val result = newMarketOrder(OrderSide.BUY, amount)
        val slippage = buySlippage(amount, result)
        log.info("Buy $amount $name:\n$result\nslippage $slippage")
    }

    override suspend fun sell(amount: BigDecimal) {
        val result = newMarketOrder(OrderSide.SELL, amount)
        val slippage = sellSlippage(amount, result)
        log.info("Sell $amount $name:\n$result\nslippage $slippage")
    }

    private suspend fun newMarketOrder(side: OrderSide, amount: BigDecimal): NewOrderResponse {
        if (amount < BigDecimal.ZERO) {
            throw MarketBroker.Error.WrongAmount()
        }
        return  try {
            // todo брать время с сервера
            api.newOrder(name, side, OrderType.MARKET, null, amount.toString(), null, null, null, 10000, Instant.now().toEpochMilli())
        } catch (e: BinanceApiException) {
            val msg = e.error.msg
            val newException = when {
                msg.startsWith("Invalid quantity") -> MarketBroker.Error.WrongAmount()
                msg.startsWith("Filter failure: LOT_SIZE") -> MarketBroker.Error.WrongAmount()
                msg.startsWith("Account has insufficient balance for requested action") -> MarketBroker.Error.InsufficientBalance()
                else -> e
            }
            throw newException
        }
    }

    private fun buySlippage(amount: BigDecimal, result: NewOrderResponse): BigDecimal {
        val desiredSellingAmount = BigDecimal(result.fills.first().price) * amount
        val factSellingAmount = result.fills.map { BigDecimal(it.qty) * BigDecimal(it.price) }.sum()
        return desiredSellingAmount.divide(factSellingAmount, 30, RoundingMode.HALF_UP).setScale(10, RoundingMode.HALF_UP)
    }

    private fun sellSlippage(amount: BigDecimal, result: NewOrderResponse): BigDecimal {
        val desiredBuyingAmount = BigDecimal(result.fills.first().price) * amount
        val factBuyingAmount = result.fills.map { BigDecimal(it.qty) * BigDecimal(it.price) }.sum()
        return factBuyingAmount.divide(desiredBuyingAmount, 30, RoundingMode.HALF_UP).setScale(10, RoundingMode.HALF_UP)
    }
}