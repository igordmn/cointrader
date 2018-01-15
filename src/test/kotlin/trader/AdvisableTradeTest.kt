package trader

import adviser.CoinPortions
import adviser.TradeAdviser
import exchange.*
import exchange.test.TestMarketBroker
import exchange.test.TestPortfolio
import exchange.test.TestTime
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TestMarketBrokerSpec : StringSpec({
    val currentTime = ZonedDateTime.of(2018, 1, 10, 1, 1, 13, 12, ZoneOffset.UTC).toInstant()

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

    fun candle(closePrice: BigDecimal) = Candle(closePrice, closePrice, closePrice, closePrice)

    fun price(fromCoin: String, toCoin: String) = object : MarketPrice {
        override suspend fun current(): BigDecimal = when {
            fromCoin == "USDT" && toCoin == "BTC" -> BigDecimal("10000")
            fromCoin == "BTC" && toCoin == "LTC" -> BigDecimal("0.01")
            fromCoin == "BTC" && toCoin == "ETH" -> BigDecimal("0.1")
            else -> throw UnsupportedOperationException()
        }
    }
    
    fun history(price: MarketPrice, fromCoin: String, toCoin: String) = object : MarketHistory {
        override suspend fun candlesBefore(time: Instant, count: Int, period: Duration) = when {
            fromCoin == "USDT" && toCoin == "BTC" -> List(count) { candle(price.current()) }
            fromCoin == "BTC" && toCoin == "LTC" -> List(count) { candle(price.current()) }
            fromCoin == "BTC" && toCoin == "ETH" -> List(count) { candle(price.current()) }
            else -> throw UnsupportedOperationException()
        }
    }
    
    fun market(portfolio: TestPortfolio, fromCoin: String, toCoin: String): Market {
        val price = price(fromCoin, toCoin)
        return Market(
                TestMarketBroker(fromCoin, toCoin, portfolio, price, BigDecimal.ZERO),
                history(price, fromCoin, toCoin),
                price
        )
    }
    val portfolio = TestPortfolio(mapOf(
            "BTC" to BigDecimal("1.0"),
            "XRP" to BigDecimal("10.0"),
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
    val time = TestTime(currentTime)
    val adviser = TestAdviser()
    val trade = AdvisableTrade(
            mainCoin = "BTC",
            altCoins = listOf("USDT", "LTC", "ETH"),
            period = Duration.ofMinutes(30),
            historyCount = 120,
            time = time,
            adviser = adviser,
            markets = markets,
            portfolio = portfolio,
            operationScale = 30
    )

    "perform multiple trades" {
        runBlocking {
            adviser.advisableCoin = "LTC"
            trade.perform()
            val amounts = portfolio.amounts()
            amounts shouldBe mapOf(
                    "BTC" to BigDecimal("0.0"),
                    "XRP" to BigDecimal("10.0"),
                    "NEO" to BigDecimal("0.0"),
                    "LTC" to BigDecimal("100.0")
            )
        }
    }
})