package exchange.test

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal

class TestMarketOrdersSpec : StringSpec({
    val ltcInBtcPrice = TestMarketPrice(BigDecimal("0.01"))
    val ethInBtcPrice = TestMarketPrice(BigDecimal("0.1"))
    val portfolio = TestPortfolio(mapOf(
            "BTC" to BigDecimal("1.0"),
            "LTC" to BigDecimal("20.0"),
            "ETH" to BigDecimal("25.0")
    ))
    val btcToLtc = TestMarketOrders("BTC", "LTC", portfolio, ltcInBtcPrice)
    val btcToEth = TestMarketOrders("BTC", "ETH", portfolio, ethInBtcPrice)

    "buy LTC" {
        runBlocking {
            btcToLtc.buy(BigDecimal(10.0))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.90")
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("30.00")
            amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("25.00")
        }
    }

    "sell LTC" {
        runBlocking {
            btcToLtc.sell(BigDecimal(5.0))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.05")
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("15.00")
            amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("25.00")
        }
    }

    "sell LTC, buy ETH" {
        runBlocking {
            btcToLtc.sell(BigDecimal(10.0))
            btcToEth.buy(BigDecimal(5.0))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.60")  // 1.0 + 10 * 0.01 - 0.1 * 5
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("10.00")
            amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("30.00")
        }
    }

    "sell LTC greater than can sell" {
        runBlocking {
            btcToLtc.sell(BigDecimal(1000.0))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.20")
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("0.00")
            amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("25.00")
        }
    }

    "buy LTC greater than can buy" {
        runBlocking {
            btcToLtc.buy(BigDecimal(1000.0))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.00")
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("120.00")
            amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("25.00")
        }
    }
})