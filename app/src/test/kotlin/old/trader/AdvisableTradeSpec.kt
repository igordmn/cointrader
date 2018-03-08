package old.trader

import adviser.CoinPortions
import adviser.TradeAdviser
import old.exchange.*
import old.exchange.candle.Candle
import old.exchange.candle.CoinToCandles
import old.exchange.candle.TimedCandle
import old.exchange.history.MarketHistory
import old.exchange.test.TestMarketBroker
import old.exchange.test.TestMarketLimits
import old.exchange.test.TestPortfolio
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.runBlocking
import com.dmi.util.lang.truncatedTo
import com.dmi.util.lang.unsupportedOperation
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AdvisableTradeSpec : StringSpec({
    val period = Duration.ofMinutes(30)
    val tradeTime = ZonedDateTime.of(2018, 1, 10, 1, 1, 13, 12, ZoneOffset.UTC).toInstant().truncatedTo(period)

    fun Map<String, BigDecimal>.round(scale: Int) = mapValues { it.value.setScale(scale) }

    class TestAdviser : TradeAdviser {
        var advisableCoin: String = ""

        override suspend fun bestPortfolioPortions(currentPortions: CoinPortions, previousCandles: CoinToCandles): CoinPortions {
            val newPortions = HashMap(currentPortions)
            for (key in newPortions.keys) {
                newPortions[key] = BigDecimal.ZERO
            }
            newPortions[advisableCoin] = BigDecimal.ONE
            return newPortions
        }
    }

    fun candle(closePrice: BigDecimal) = TimedCandle(
            Instant.ofEpochMilli(0)..Instant.ofEpochMilli(1),
            Candle(closePrice, closePrice, closePrice, closePrice)
    )

    fun price(fromCoin: String, toCoin: String) = object : MarketPrice {
        override suspend fun current(): BigDecimal = when {
            fromCoin == "USDT" && toCoin == "BTC" -> BigDecimal("10000")
            fromCoin == "BTC" && toCoin == "LTC" -> BigDecimal("0.01")
            fromCoin == "BTC" && toCoin == "ETH" -> BigDecimal("0.1")
            else -> unsupportedOperation()
        }
    }
    
    fun history(price: MarketPrice, fromCoin: String, toCoin: String) = object : MarketHistory {
        override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
            send(when {
                fromCoin == "USDT" && toCoin == "BTC" -> candle(price.current())
                fromCoin == "BTC" && toCoin == "LTC" -> candle(price.current())
                fromCoin == "BTC" && toCoin == "ETH" -> candle(price.current())
                else -> unsupportedOperation()
            })
        }
    }
    
    fun market(portfolio: TestPortfolio, fromCoin: String, toCoin: String): Market {
        val price = price(fromCoin, toCoin)
        return Market(
                TestMarketBroker(
                        fromCoin, toCoin, portfolio, price, BigDecimal.ZERO, TestMarketLimits(BigDecimal.ZERO, BigDecimal.ZERO),
                        TestMarketBroker.EmptyListener()),
                history(price, fromCoin, toCoin),
                price
        )
    }
    val portfolio = TestPortfolio(mapOf(
            "BTC" to BigDecimal("1.0"),
            "XRP" to BigDecimal("10.0"),
            "ETH" to BigDecimal("5.0"),
            "NEO" to BigDecimal("0.0")
    ))
    val markets = object : Markets {
        override fun of(fromCoin: String, toCoin: String): Market? = when {
            fromCoin == "USDT" && toCoin == "BTC" -> market(portfolio, fromCoin, toCoin)
            fromCoin == "BTC" && toCoin == "LTC" -> market(portfolio, fromCoin, toCoin)
            fromCoin == "BTC" && toCoin == "ETH" -> market(portfolio, fromCoin, toCoin)
            else -> null
        }
    }
    val adviser = TestAdviser()
    val trade = AdvisableTrade(
            mainCoin = "BTC",
            altCoins = listOf("USDT", "LTC", "ETH"),
            historyCount = 120,
            adviser = adviser,
            markets = markets,
            portfolio = portfolio,
            operationScale = 30,
            listener = AdvisableTrade.EmptyListener()
    )

    "perform multiple trades" {
        runBlocking {
            adviser.advisableCoin = "LTC"
            trade.perform(tradeTime)
            portfolio.amounts().round(scale = 2) shouldBe mapOf(
                    "BTC" to BigDecimal("0.00"),
                    "XRP" to BigDecimal("10.00"),
                    "NEO" to BigDecimal("0.00"),
                    "ETH" to BigDecimal("5.00"),
                    "LTC" to BigDecimal("100.00")
            )

            adviser.advisableCoin = "LTC"
            trade.perform(tradeTime)
            portfolio.amounts().round(scale = 2) shouldBe mapOf(
                    "BTC" to BigDecimal("0.00"),
                    "XRP" to BigDecimal("10.00"),
                    "NEO" to BigDecimal("0.00"),
                    "ETH" to BigDecimal("5.00"),
                    "LTC" to BigDecimal("100.00")
            )

            adviser.advisableCoin = "ETH"
            trade.perform(tradeTime)
            portfolio.amounts().round(scale = 2) shouldBe mapOf(
                    "BTC" to BigDecimal("0.00"),
                    "XRP" to BigDecimal("10.00"),
                    "NEO" to BigDecimal("0.00"),
                    "ETH" to BigDecimal("15.00"),
                    "LTC" to BigDecimal("0.00")
            )

            adviser.advisableCoin = "BTC"
            trade.perform(tradeTime)
            portfolio.amounts().round(scale = 2) shouldBe mapOf(
                    "BTC" to BigDecimal("1.50"),
                    "XRP" to BigDecimal("10.00"),
                    "NEO" to BigDecimal("0.00"),
                    "ETH" to BigDecimal("0.00"),
                    "LTC" to BigDecimal("0.00")
            )

            adviser.advisableCoin = "BTC"
            trade.perform(tradeTime)
            portfolio.amounts().round(scale = 2) shouldBe mapOf(
                    "BTC" to BigDecimal("1.50"),
                    "XRP" to BigDecimal("10.00"),
                    "NEO" to BigDecimal("0.00"),
                    "ETH" to BigDecimal("0.00"),
                    "LTC" to BigDecimal("0.00")
            )
        }
    }
})