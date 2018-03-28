package com.dmi.cointrader.test

import com.dmi.cointrader.broker.Broker
import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldThrow
import kotlinx.coroutines.experimental.runBlocking
import java.math.BigDecimal

class TestExchangeSpec : Spec({
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
            btcToLtc.buy(BigDecimal("10.0"))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.90")
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("30.00")
            amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("25.00")
        }

        "sell LTC" {
            btcToLtc.sell(BigDecimal("5.0"))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("1.05")
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("15.00")
            amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("25.00")
        }

        "sell LTC, buy ETH" {
            btcToLtc.sell(BigDecimal("10.0"))
            btcToEth.buy(BigDecimal("5.0"))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.60")  // 1.0 + 10 * 0.01 - 0.1 * 5
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("10.00")
            amounts["ETH"]!!.setScale(2) shouldBe BigDecimal("30.00")
        }

        "cannot buy/sell zero/negative amount" {
            shouldThrow<Broker.OrderError.WrongAmount> {
                btcToLtc.buy(BigDecimal("0.0"))
            }
            shouldThrow<Broker.OrderError.WrongAmount> {
                btcToLtc.sell(BigDecimal("0.0"))
            }
            shouldThrow<Broker.OrderError.WrongAmount> {
                btcToLtc.buy(BigDecimal("-10.0"))
            }
            shouldThrow<Broker.OrderError.WrongAmount> {
                btcToLtc.sell(BigDecimal("-10.0"))
            }
        }

        "sell/buy LTC greater than can sell/buy" {
            shouldThrow<Broker.OrderError.InsufficientBalance> {
                btcToLtc.sell(BigDecimal("21.0"))
            }
            shouldThrow<Broker.OrderError.InsufficientBalance> {
                btcToLtc.buy(BigDecimal("101.0"))
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
            btcToLtc.buy(BigDecimal("10.0"))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(2) shouldBe BigDecimal("0.90")
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("29.00")
        }

        "sell LTC" {
            btcToLtc.sell(BigDecimal("5.0"))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(3) shouldBe BigDecimal("1.045")
            amounts["LTC"]!!.setScale(2) shouldBe BigDecimal("15.00")
        }
    }

    "zero fee, with limits" - {
        val portfolio = TestPortfolio(mapOf(
                "BTC" to BigDecimal("1.0"),
                "LTC" to BigDecimal("20.0")
        ))
        val limits = TestMarketLimits(BigDecimal("0.1"), BigDecimal("0.02"))
        val btcToLtc = TestMarketBroker(
                "BTC", "LTC", portfolio, ltcInBtcPrice, BigDecimal.ZERO, limits,
                TestMarketBroker.EmptyListener()
        )

        "can buy LTC greater than min limit and multiply of step" {
            btcToLtc.buy(BigDecimal("0.1"))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(4) shouldBe BigDecimal("0.9990")
            amounts["LTC"]!!.setScale(4) shouldBe BigDecimal("20.1000")
        }

        "can sell LTC greater than min limit and multiply of step" {
            btcToLtc.sell(BigDecimal("0.1"))
            val amounts = portfolio.amounts()
            amounts["BTC"]!!.setScale(4) shouldBe BigDecimal("1.0010")
            amounts["LTC"]!!.setScale(4) shouldBe BigDecimal("19.9000")
        }

        "cannot buy LTC less than min limit" {
            shouldThrow<Broker.OrderError.WrongAmount> {
                btcToLtc.buy(BigDecimal("0.08"))
            }
        }

        "cannot buy/sell LTC that not multiple of step" {
            shouldThrow<Broker.OrderError.WrongAmount> {
                btcToLtc.buy(BigDecimal("0.11"))
            }
            shouldThrow<Broker.OrderError.WrongAmount> {
                btcToLtc.sell(BigDecimal("0.11"))
            }
        }
    }
})