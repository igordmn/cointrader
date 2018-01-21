package exchange.test

import exchange.MarketBroker
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal

// todo Exception, если нет средств. Если меньше минимального, или не совпадает с шагом
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
                btcToLtc.buy(BigDecimal("10.0"))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.90")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("30.00")
                amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("25.00")
            }
        }

        "sell LTC" {
            runBlocking {
                btcToLtc.sell(BigDecimal("5.0"))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.05")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("15.00")
                amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("25.00")
            }
        }

        "sell LTC, buy ETH" {
            runBlocking {
                btcToLtc.sell(BigDecimal("10.0"))
                btcToEth.buy(BigDecimal("5.0"))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.60")  // 1.0 + 10 * 0.01 - 0.1 * 5
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("10.00")
                amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("30.00")
            }
        }

        "cannot buy/sell zero/negative amount" {
            runBlocking {
                shouldThrow<MarketBroker.Error.WrongAmount> {
                    btcToLtc.buy(BigDecimal("0.0"))
                }
                shouldThrow<MarketBroker.Error.WrongAmount> {
                    btcToLtc.sell(BigDecimal("0.0"))
                }
                shouldThrow<MarketBroker.Error.WrongAmount> {
                    btcToLtc.buy(BigDecimal("-10.0"))
                }
                shouldThrow<MarketBroker.Error.WrongAmount> {
                    btcToLtc.sell(BigDecimal("-10.0"))
                }
            }
        }

        "sell/buy LTC greater than can sell/buy" {
            runBlocking {
                shouldThrow<MarketBroker.Error.InsufficientBalance> {
                    btcToLtc.sell(BigDecimal("21.0"))
                }
                shouldThrow<MarketBroker.Error.InsufficientBalance> {
                    btcToLtc.buy(BigDecimal("101.0"))
                }
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
                btcToLtc.buy(BigDecimal("10.0"))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.90")
                amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("29.00")
            }
        }

        "sell LTC" {
            runBlocking {
                btcToLtc.sell(BigDecimal("5.0"))
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
                "BTC", "LTC", portfolio, ltcInBtcPrice, BigDecimal.ZERO, TestMarketLimits(BigDecimal("0.1"), BigDecimal("0.02")),
                TestMarketBroker.EmptyListener()
        )

        "can buy LTC greater than min limit and multiply of step" {
            runBlocking {
                btcToLtc.buy(BigDecimal("0.1"))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(4) shouldBe BigDecimal("0.9990")
                amounts["LTC"]!!.setScale(4) shouldBe BigDecimal("20.1000")
            }
        }

        "can sell LTC greater than min limit and multiply of step" {
            runBlocking {
                btcToLtc.sell(BigDecimal("0.1"))
                val amounts = portfolio.amounts()
                amounts["BTC"]!!.setScale(4) shouldBe BigDecimal("1.0010")
                amounts["LTC"]!!.setScale(4) shouldBe BigDecimal("19.9000")
            }
        }

        "cannot buy LTC less than min limit" {
            runBlocking {
                shouldThrow<MarketBroker.Error.WrongAmount> {
                    btcToLtc.buy(BigDecimal("0.08"))
                }
            }
        }

        "cannot buy/sell LTC that not multiple of step" {
            runBlocking {
                shouldThrow<MarketBroker.Error.WrongAmount> {
                    btcToLtc.buy(BigDecimal("0.11"))
                }
                shouldThrow<MarketBroker.Error.WrongAmount> {
                    btcToLtc.sell(BigDecimal("0.11"))
                }
            }
        }
    }
})