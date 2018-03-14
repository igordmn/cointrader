package com.dmi.cointrader.app.binance

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.market.AggTrade
import com.binance.api.client.exception.BinanceApiException
import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.cointrader.app.binance.api.binanceAPI
import com.dmi.cointrader.app.binance.api.model.NewOrderResponse
import com.dmi.util.log.logger
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import java.io.File
import java.math.BigDecimal
import java.time.Instant

typealias Asset = String
typealias Portfolio = Map<Asset, BigDecimal>

fun prodBinanceExchange(): BinanceExchange {
    val apiKey = File("E:/Distr/Data/CryptoExchanges/binance/apiKey.txt").readText()
    val secret = File("E:/Distr/Data/CryptoExchanges/binance/secret.txt").readText()
    val api = binanceAPI(apiKey, secret, logger("BinanceAPI"), maxRequestsPerSecond = 10)
    return BinanceExchange(api)
}

fun testBinanceExchange(): BinanceExchange {
    val api = binanceAPI()
    return BinanceExchange(api)
}

class BinanceExchange(
        private val api: BinanceAPI
) {
    private val btcReversedMarkets = setOf("USDT")

    suspend fun currentTime(): Instant = Instant.ofEpochSecond(api.serverTime().serverTime)

    suspend fun portfolio(timestamp: Instant): Portfolio {
        val result = api.getAccount(5000, timestamp.toEpochMilli())
        return result.balances.associate {
            it.asset to BigDecimal(it.free)
        }
    }

    suspend fun market(fromAsset: Asset, toAsset: Asset, chunkLoadCount: Int = 500): Market? {
        val name = marketName(fromAsset, toAsset)
        return name?.let { Market(it, chunkLoadCount) }
    }

    private fun marketName(fromAsset: Asset, toAsset: Asset): String? {
        return when {
            toAsset == "BTC" && fromAsset in btcReversedMarkets -> "BTC$fromAsset"
            else -> "${toAsset}BTC"
        }
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

        suspend fun buy(amount: BigDecimal, timestamp: Instant): OrderResult {
            val result = newMarketOrder(OrderSide.BUY, amount, timestamp)
            return OrderResult(buySlippage(amount, result))
        }

        suspend fun sell(amount: BigDecimal, timestamp: Instant): OrderResult {
            val result = newMarketOrder(OrderSide.SELL, amount, timestamp)
            return OrderResult(sellSlippage(amount, result))
        }

        private suspend fun newMarketOrder(side: OrderSide, amount: BigDecimal, timestamp: Instant): NewOrderResponse {
            if (amount < BigDecimal.ZERO) {
                throw Error.WrongAmount
            }
            return try {
                api.newOrder(name, side, OrderType.MARKET, null, amount.toString(), null, null, null, 5000, timestamp.toEpochMilli())
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