package com.dmi.cointrader.binance

import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.general.FilterType
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.AggTrade
import com.binance.api.client.domain.market.TickerPrice
import com.binance.api.client.exception.BinanceApiException
import com.dmi.cointrader.binance.api.BinanceAPI
import com.dmi.cointrader.binance.api.binanceAPI
import com.dmi.cointrader.binance.api.model.NewOrderResponse
import com.dmi.cointrader.broker.Broker
import com.dmi.cointrader.broker.Broker.Limits
import com.dmi.cointrader.broker.Broker.OrderError
import com.dmi.cointrader.broker.Broker.OrderResult
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import org.slf4j.Logger
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

typealias Asset = String
typealias Portfolio = Map<Asset, BigDecimal>
typealias AssetPrices = Map<Asset, BigDecimal>
typealias Amounts = List<BigDecimal>

fun Portfolio.amountsOf(assets: List<Asset>): Amounts = assets.map { this[it]!! }

suspend fun binanceExchangeForTrade(log: Logger): BinanceExchange {
    val keyPaths = File("data/keys.txt").readLines()
    val apiKey = File(keyPaths[0]).readText()
    val secret = File(keyPaths[1]).readText()
    val api = binanceAPI(apiKey, secret, log, maxRequestsPerSecond = 10)
    val info = info(api)
    return BinanceExchange(api, info)
}

suspend fun binanceExchangeForInfo(): BinanceExchange {
    val api = binanceAPI(maxRequestsPerSecond = 6)
    val info = info(api)
    return BinanceExchange(api, info)
}

private suspend fun info(api: BinanceAPI): BinanceExchange.Info {
    fun SymbolInfo.limits(): Limits {
        val lotSizeFilter = filters.find { it.filterType == FilterType.LOT_SIZE }!!
        return Limits(
                minAmount = BigDecimal(lotSizeFilter.minQty),
                amountStep = BigDecimal(lotSizeFilter.stepSize)
        )
    }

    val info = api.exchangeInfo()
    return BinanceExchange.Info(
            markets = info.symbols.map(SymbolInfo::getSymbol).toSet(),
            marketToLimits =  info.symbols.associate { it.symbol to it.limits() }
    )
}

class BinanceExchange(private val api: BinanceAPI, private val info: Info) {
    suspend fun currentTime(): Instant = Instant.ofEpochSecond(api.serverTime().serverTime)

    suspend fun portfolio(clock: Clock): Portfolio {
        val result = api.getAccount(5000, clock.instant().toEpochMilli())
        return result.balances.associate {
            it.asset to BigDecimal(it.free)
        }
    }

    suspend fun btcPrices(): AssetPrices {
        fun TickerPrice.toAssetPrice(): Pair<Asset, BigDecimal> {
            val isReversed = symbol.endsWith("BTC")
            val price = price.toBigDecimal()
            val btcPrice = if (isReversed) BigDecimal.ONE.divide(price, 8) else price
            val asset = if (isReversed) symbol.removeSuffix("BTC") else symbol.removePrefix("BTC")
            return asset to btcPrice
        }

        return api
                .allPrices()
                .filter { it.symbol.startsWith("BTC") || it.symbol.endsWith("BTC") }
                .associate(TickerPrice::toAssetPrice)
    }

    fun market(baseAsset: Asset, quoteAsset: Asset): Market? {
        val normalName = "$baseAsset$quoteAsset"
        val reversedName = "$quoteAsset$baseAsset"
        return when  {
            info.markets.contains(normalName) -> Market(normalName)
            info.markets.contains(reversedName) -> Market(reversedName)
            else -> null
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
                price.toBigDecimal(),
                quantity.toBigDecimal()
        )

        fun broker(clock: Clock) = object : Broker {
            override val limits: Limits = info.marketToLimits[name]!!

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

    data class Trade(val time: Instant, val aggTradeId: Long, val price: BigDecimal, val amount: BigDecimal)
    data class Info(val markets: Set<String>, val marketToLimits: Map<String, Limits>)
}