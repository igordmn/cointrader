package com.dmi.cointrader.app.binance

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.market.AggTrade
import com.binance.api.client.exception.BinanceApiException
import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.cointrader.app.binance.api.model.NewOrderResponse
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import java.math.BigDecimal
import java.time.Instant

class BinanceExchange(
        private val api: BinanceAPI
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

        suspend fun buy(amount: BigDecimal): OrderResult {
            val result = newMarketOrder(OrderSide.BUY, amount)
            return OrderResult(buySlippage(amount, result))
        }

        suspend fun sell(amount: BigDecimal): OrderResult {
            val result = newMarketOrder(OrderSide.SELL, amount)
            return OrderResult(sellSlippage(amount, result))
        }

        private suspend fun newMarketOrder(side: OrderSide, amount: BigDecimal): NewOrderResponse {
            if (amount < BigDecimal.ZERO) {
                throw Error.WrongAmount
            }
            return try {
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

        private fun buySlippage(amount: BigDecimal, result: NewOrderResponse): Double {
            val desiredSellingAmount = result.fills.first().price!!.toDouble() * amount.toDouble()
            val factSellingAmount = result.fills.map { it.qty!!.toDouble() * it.price!!.toDouble() }.sum()
            return desiredSellingAmount / factSellingAmount
        }

        private fun sellSlippage(amount: BigDecimal, result: NewOrderResponse): Double {
            val desiredSellingAmount = result.fills.first().price!!.toDouble() * amount.toDouble()
            val factSellingAmount = result.fills.map { it.qty!!.toDouble() * it.price!!.toDouble() }.sum()
            return factSellingAmount / desiredSellingAmount
        }
    }

    sealed class Error : Exception() {
        object InsufficientBalance : Error()
        object WrongAmount : Error()
    }

    data class Trade(val time: Instant, val aggTradeId: Long, val amount: BigDecimal, val price: BigDecimal)
    data class OrderResult(val slippage: Double)
}