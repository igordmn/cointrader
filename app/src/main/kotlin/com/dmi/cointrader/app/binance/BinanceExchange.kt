package com.dmi.cointrader.app.binance

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.market.AggTrade
import com.binance.api.client.exception.BinanceApiException
import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.cointrader.app.binance.api.binanceAPI
import com.dmi.cointrader.app.binance.api.model.NewOrderResponse
import com.dmi.util.math.sum
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import org.slf4j.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class BinanceExchange(
        private val api: BinanceAPI,
        private val log: Logger
) {
    private val constants = BinanceConstants()

    suspend fun currentTime(): Instant = TODO()

    suspend fun assets(): Assets {
        // todo брать время с сервера
        val result = api.getAccount(10000, Instant.now().toEpochMilli())
        return Assets(
                map = result.balances.associate {
                    it.asset to BigDecimal(it.free)
                }
        )
    }

    suspend fun market(fromAsset: String, toAsset: String, chunkLoadCount: Int = 500): Market? {
        val name = constants.marketName(fromAsset, toAsset)
        return name?.let { Market(it, chunkLoadCount) }
    }

    inner class Assets(private val map: Map<String, BigDecimal>) {
        fun amountOf(coin: String): BigDecimal = map[coin]!!
    }

    inner class Market(private val name: String, private val chunkLoadCount: Int) {
        fun trades(startId: Long): ReceiveChannel<Trade> = produce {
            var id = startId

            while (true) {
                val trades = api.getAggTrades(name, id.toString(), chunkLoadCount, null, null)
                if (trades.isNotEmpty()) {
                    trades.forEach {
                        send(it.convert())
                    }
                    id = trades.last().aggregatedTradeId + 1
                } else {
                    break
                }
            }
        }

        private fun AggTrade.convert() = Trade(
                Instant.ofEpochMilli(tradeTime),
                aggregatedTradeId,
                quantity.toBigDecimal(),
                price.toBigDecimal()
        )

        suspend fun buy(amount: BigDecimal) {
            val result = newMarketOrder(OrderSide.BUY, amount)
            val slippage = buySlippage(amount, result)
            log.info("Buy $amount $name:\n$result\nslippage $slippage")
        }

        suspend fun sell(amount: BigDecimal) {
            val result = newMarketOrder(OrderSide.SELL, amount)
            val slippage = sellSlippage(amount, result)
            log.info("Sell $amount $name:\n$result\nslippage $slippage")
        }

        private suspend fun newMarketOrder(side: OrderSide, amount: BigDecimal): NewOrderResponse {
            if (amount < BigDecimal.ZERO) {
                throw Error.WrongAmount
            }
            return  try {
                // todo брать время с сервера
                api.newOrder(name, side, OrderType.MARKET, null, amount.toString(), null, null, null, 10000, Instant.now().toEpochMilli())
            } catch (e: BinanceApiException) {
                val msg = e.error.msg
                val newException = when {
                    msg.startsWith("Invalid quantity") -> Error.WrongAmount
                    msg.startsWith("Filter failure: LOT_SIZE") -> Error.WrongAmount
                    msg.startsWith("Account has insufficient balance for requested action") -> Error.InsufficientBalance
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

    sealed class Error : Exception() {
        object InsufficientBalance : Error()
        object WrongAmount : Error()
    }

    data class Trade(val time: Instant, val aggTradeId: Long, val amount: BigDecimal, val price: BigDecimal)
}