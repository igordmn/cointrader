package com.dmi.cointrader.app.binance

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.market.AggTrade
import com.binance.api.client.exception.BinanceApiException
import com.dmi.cointrader.app.binance.api.BinanceAPI
import com.dmi.cointrader.app.binance.api.binanceAPI
import com.dmi.cointrader.app.binance.api.model.NewOrderResponse
import com.dmi.cointrader.app.broker.Broker
import com.dmi.cointrader.app.broker.Broker.Limits
import com.dmi.cointrader.app.broker.Broker.OrderError
import com.dmi.cointrader.app.broker.Broker.OrderResult
import com.dmi.cointrader.app.broker.OrderError
import com.dmi.cointrader.app.broker.OrderLimits
import com.dmi.cointrader.app.broker.OrderResult
import com.dmi.util.log.logger
import com.google.common.collect.BiMap
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

typealias Asset = String
typealias Portfolio = Map<Asset, BigDecimal>
typealias Amounts = List<BigDecimal>

fun Portfolio.amountsOf(assets: List<Asset>): Amounts = assets.map { this[it]!! }

suspend fun productionBinanceExchange(): BinanceExchange {
    val apiKey = File("E:/Distr/Data/CryptoExchanges/binance/apiKey.txt").readText()
    val secret = File("E:/Distr/Data/CryptoExchanges/binance/secret.txt").readText()
    val api = binanceAPI(apiKey, secret, logger("BinanceAPI"), maxRequestsPerSecond = 10)
    val info = api.exchangeInfo()
    info.symbols.
            return BinanceExchange(api)
}

fun testBinanceExchange(): BinanceExchange {
    val api = binanceAPI()
    return BinanceExchange(api)
}

class BinanceExchange(private val api: BinanceAPI, private val info: Info) {
    suspend fun currentTime(): Instant = Instant.ofEpochSecond(api.serverTime().serverTime)

    suspend fun portfolio(timestamp: Instant): Portfolio {
        val result = api.getAccount(5000, timestamp.toEpochMilli())
        return result.balances.associate {
            it.asset to BigDecimal(it.free)
        }
    }

    fun market(mainAsset: Asset, toAsset: Asset): Market? {
        val name = marketName(mainAsset, toAsset)
        return name?.let(::Market)
    }

    private fun marketName(mainAsset: Asset, toAsset: Asset): String? {
        return when {
            toAsset == "BTC" && mainAsset in info.reversedMarkets -> "BTC$mainAsset"
            else -> "${toAsset}BTC"
        }
    }

    inner class Market(private val name: String) {
        fun trades(startId: Long, chunkLoadCount: Int = 500): ReceiveChannel<Trade> = produce {
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

        fun broker(clock: Clock) = object : Broker {
            override val limits: Limits
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

            override suspend fun buy(amount: BigDecimal): OrderResult {
                val result = newMarketOrder(OrderSide.BUY, amount, clock.instant())
                return OrderResult(buySlippage(amount, result))
            }

            override suspend fun sell(amount: BigDecimal): OrderResult {
                val result = newMarketOrder(OrderSide.SELL, amount, clock.instant())
                return OrderResult(sellSlippage(amount, result))
            }

            private suspend fun newMarketOrder(side: OrderSide, amount: BigDecimal, timestamp: Instant): NewOrderResponse {
                if (amount < BigDecimal.ZERO) {
                    throw OrderError.WrongAmount
                }
                return try {
                    api.newOrder(name, side, OrderType.MARKET, null, amount.toString(), null, null, null, 5000, timestamp.toEpochMilli())
                } catch (e: BinanceApiException) {
                    val msg = e.error.msg
                    val newException = when {
                        msg.startsWith("Invalid quantity") -> OrderError.WrongAmount
                        msg.startsWith("Filter failure: LOT_SIZE") -> OrderError.WrongAmount
                        msg.startsWith("Account has insufficient balance for requested action") -> OrderError.InsufficientBalance
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
    }

    data class Trade(val time: Instant, val aggTradeId: Long, val amount: BigDecimal, val price: BigDecimal)
    data class Info(val markets: BiMap<Asset, Asset>, val precisions: Map<Asset, Int>)
    private data class MarketInfo(val precisions: Map<Asset, Int>)
}