package exchange.test

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal

class TestMarketBrokerSpec : FreeSpec({
    val ltcInBtcPrice = TestMarketPrice(BigDecimal("0.01"))
    val ethInBtcPrice = TestMarketPrice(BigDecimal("0.1"))

    "zero fee, zero limits" - {
        val portfolio = TestPortfolio(mapOf(
                "BTC" to BigDecimal("1.0"),
                "LTC" to BigDecimal("20.0"),
                "ETH" to BigDecimal("25.0")
        ))
        val btcToLtc = TestMarketBroker(
                "BTC", "LTC", portfolio, ltcInBtcPrice, BigDecimal.ZERO, TestMarketLimits(BigDecimal.ZERO, BigDecimal.ZERO),
                TestMarketBroker.EmptyListener()
        )
        val btcToEth = TestMarketBroker(
                "BTC", "ETH", portfolio, ethInBtcPrice, BigDecimal.ZERO, TestMarketLimits(BigDecimal.ZERO, BigDecimal.ZERO),
                TestMarketBroker.EmptyListener()
        )

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
    }

    "0.1 fee, zero limits" - {
        val portfolio = TestPortfolio(mapOf(
                "BTC" to BigDecimal("1.0"),
                "LTC" to BigDecimal("20.0")
        ))
        val btcToLtc = TestMarketBroker(
                "BTC", "LTC", portfolio, ltcInBtcPrice, BigDecimal("0.1"), TestMarketLimits(BigDecimal.ZERO, BigDecimal.ZERO),
                TestMarketBroker.EmptyListener()
        )

        "buy LTC" {
            runBlocking {
                btcToLtc.buy(BigDecimal(10.0))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.90")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("29.00")
            }
        }

        "sell LTC" {
            runBlocking {
                btcToLtc.sell(BigDecimal(5.0))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(3) shouldBe BigDecimal("1.045")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("15.00")
            }
        }
    }

    "zero fee, with limits" - {
        val portfolio = TestPortfolio(mapOf(
                "BTC" to BigDecimal("1.0"),
                "LTC" to BigDecimal("20.0")
        ))
        val btcToLtc = TestMarketBroker(
                "BTC", "LTC", portfolio, ltcInBtcPrice, BigDecimal.ZERO, TestMarketLimits(BigDecimal("0.1"), BigDecimal("0.01")),
                TestMarketBroker.EmptyListener()
        )

        "not buying any LTC if less than limit" {
            runBlocking {
                btcToLtc.buy(BigDecimal(0.09))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.00")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("20.00")
            }
        }

        "not selling any LTC if less than limit" {
            runBlocking {
                btcToLtc.sell(BigDecimal(0.99))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.00")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("20.00")
            }
        }

        "not buying any LTC if less than total price limit" {
            runBlocking {
                btcToLtc.buy(BigDecimal(0.99))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.00")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("20.00")
            }
        }

        "not selling any LTC if less than total price limit" {
            runBlocking {
                btcToLtc.sell(BigDecimal(0.09))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.00")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("20.00")
            }
        }

        "buying rounded LTC if greater than limit" {
            runBlocking {
                btcToLtc.buy(BigDecimal(1.09))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.99")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("21.00")
            }
        }

        "selling rounded LTC if greater than limit" {
            runBlocking {
                btcToLtc.sell(BigDecimal(1.09))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.01")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("19.00")
            }
        }
    }
})