//package com.dmi.cointrader.broker
//
//import io.kotlintest.matchers.shouldThrow
//import io.kotlintest.specs.FreeSpec
//import kotlinx.coroutines.experimental.runBlocking
//import org.slf4j.helpers.NOPLogger
//import java.math.BigDecimal
//import java.math.RoundingMode
//
//class SafeBrokerSpec : FreeSpec({
//    val ltcInBtcPrice = TestMarketPrice(BigDecimal("0.01"))
//
//    val portfolio = TestPortfolio(mapOf(
//            "BTC" to BigDecimal("1.0"),
//            "LTC" to BigDecimal("20.0")
//    ))
//
//    val limits = Broker.Limits(minAmount = BigDecimal("0.1"), amountStep = BigDecimal("0.02"))
//    val testMarketBroker = TestMarketBroker(
//            "BTC", "LTC", portfolio, ltcInBtcPrice, BigDecimal.ZERO, limits,
//            TestMarketBroker.EmptyListener()
//    )
//
//    val safeBroker = SafeMarketBroker(testMarketBroker, limits, 3, BigDecimal("0.99"), NOPLogger.NOP_LOGGER)
//
//    "can buy/sell zero amount" {
//        runBlocking {
//            safeBroker.buy(BigDecimal("0.0"))
//            safeBroker.sell(BigDecimal("0.0"))
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.00")
//            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("20.00")
//        }
//    }
//
//    "can buy amount smaller than minimal" {
//        runBlocking {
//            safeBroker.buy(BigDecimal("0.05"))
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.00")
//            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("20.00")
//        }
//    }
//
//    "can sell amount smaller than minimal" {
//        runBlocking {
//            safeBroker.buy(BigDecimal("0.05"))
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.00")
//            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("20.00")
//        }
//    }
//
//    "can buy amount not multiply by amount step" {
//        runBlocking {
//            safeBroker.buy(BigDecimal("0.15"))
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(4) shouldBe BigDecimal("0.9986")
//            amounts["LTC"]!!.setScale(4) shouldBe BigDecimal("20.1400")
//        }
//    }
//
//    "can sell amount not multiply by amount step" {
//        runBlocking {
//            safeBroker.sell(BigDecimal("0.15"))
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("1.0014")
//            amounts["LTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("19.8600")
//        }
//    }
//
//    "decrease amount by 0.99 when first buy unsuccessful with insufficient balance" {
//        runBlocking {
//            safeBroker.buy(BigDecimal("101")) // 101 * 0.99 = 99.99, round(99.99, 0.02) = 99.98
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("0.0002")
//            amounts["LTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("119.9800")
//        }
//    }
//
//    "decrease amount by 0.99 when first and second buy unsuccessful with insufficient balance" {
//        runBlocking {
//            safeBroker.buy(BigDecimal("102")) // 101 * 0.99 * 0.99 = 99.9702  round(99.9702, 0.02) = 99.96
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("0.0004")
//            amounts["LTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("119.9600")
//        }
//    }
//
//    "fourth attempt not allowed when first, second and third buy unsuccessful with insufficient balance" {
//        runBlocking {
//            shouldThrow<MarketBroker.Error.InsufficientBalance> {
//                safeBroker.buy(BigDecimal("103")) // 101 * 0.99 * 0.99 = 100.9503 > 100
//            }
//        }
//    }
//
//    "decrease amount by 0.99 when first sell unsuccessful with insufficient balance" {
//        runBlocking {
//            safeBroker.sell(BigDecimal("20.2")) // 20.2 * 0.99 = 19.998, round(19.998, 0.02) = 19.98
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("1.1998")
//            amounts["LTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("0.0200")
//        }
//    }
//
//    "decrease amount by 0.99 when first and second sell unsuccessful with insufficient balance" {
//        runBlocking {
//            safeBroker.sell(BigDecimal("20.3")) // 20.3 * 0.99 * 0.99 = 19.89603  round(19.89603, 0.02) = 19.88
//
//            val amounts = portfolio.amounts()
//            amounts["BTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("1.1988")
//            amounts["LTC"]!!.setScale(4, RoundingMode.HALF_UP) shouldBe BigDecimal("0.1200")
//        }
//    }
//
//    "fourth attempt not allowed when first, second and third sell unsuccessful with insufficient balance" {
//        runBlocking {
//            shouldThrow<MarketBroker.Error.InsufficientBalance> {
//                safeBroker.sell(BigDecimal("20.5")) // 20.5 * 0.99 * 0.99 = 20.09205 > 20
//            }
//        }
//    }
//
//    "amount cannot be negative" {
//        shouldThrow<MarketBroker.Error.WrongAmount> {
//            runBlocking {
//                safeBroker.buy(BigDecimal("-1.0"))
//            }
//        }
//        shouldThrow<MarketBroker.Error.WrongAmount> {
//            runBlocking {
//                safeBroker.sell(BigDecimal("-1.0"))
//            }
//        }
//    }
//})