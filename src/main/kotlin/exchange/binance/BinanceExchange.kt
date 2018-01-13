package exchange.binance

import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderType
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.NewOrder
import com.binance.api.client.domain.market.Candlestick
import com.binance.api.client.domain.market.CandlestickInterval
import exchange.*
import util.lang.truncatedTo
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.experimental.suspendCoroutine

class BinanceExchange(
        private val client: BinanceApiAsyncRestClient,
        private val info: BinanceInfo
) : Exchange {
    override fun portfolio() = object : Portfolio {
        override suspend fun amounts(): Map<String, BigDecimal> = suspendCoroutine { continuation ->
            client.getAccount { account ->
                val amounts = account.balances.associate {
                    val standardName = info.binanceNameToStandard[it.asset] ?: it.asset
                    standardName to BigDecimal(it.free)
                }
                continuation.resume(amounts)
            }
        }
    }

    override fun marketFor(fromCoin: String, toCoin: String): Market? {
        val name = info.marketName(fromCoin, toCoin)
        return name?.let(this::market)
    }

    private fun market(name: String) = object : Market {
        override fun history() = object : MarketHistory {
            override suspend fun candlesBefore(time: Instant, count: Int, period: Duration): List<Candle> = suspendCoroutine { continuation ->
                fun Duration.toServerInterval() = when(this) {
                    Duration.ofMinutes(1) -> CandlestickInterval.ONE_MINUTE
                    Duration.ofMinutes(5) -> CandlestickInterval.FIVE_MINUTES
                    Duration.ofMinutes(15) -> CandlestickInterval.FIFTEEN_MINUTES
                    Duration.ofMinutes(30) -> CandlestickInterval.HALF_HOURLY
                    Duration.ofMinutes(60) -> CandlestickInterval.HOURLY
                    else -> throw UnsupportedOperationException()
                }

                fun Candlestick.toLocalCandle() = Candle(
                        BigDecimal(close),
                        BigDecimal(open),
                        BigDecimal(high),
                        BigDecimal(low)
                )

                val endOfPeriod = time.truncatedTo(period).epochSecond - 1
                client.getCandlestickBars(name, period.toServerInterval(), count, null, endOfPeriod) { result ->
                    require(result.last().closeTime == endOfPeriod)
                    continuation.resume(result.map(Candlestick::toLocalCandle))
                }
            }
        }

        override suspend fun buy(amount: BigDecimal) = suspendCoroutine<Unit> { continuation ->
            val order = NewOrder(name, OrderSide.BUY, OrderType.MARKET, TimeInForce.IOC, amount.toString())
            client.newOrder(order) { response ->
                continuation.resume(Unit)
            }
        }

        override suspend fun sell(amount: BigDecimal) = suspendCoroutine<Unit> { continuation ->
            val order = NewOrder(name, OrderSide.SELL, OrderType.MARKET, TimeInForce.IOC, amount.toString())
            client.newOrder(order) { response ->
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun currentTime(): Instant = suspendCoroutine { continuation ->
        client.getServerTime {
            continuation.resume(Instant.ofEpochSecond(it.serverTime))
        }
    }
}