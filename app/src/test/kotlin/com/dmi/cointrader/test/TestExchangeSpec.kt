package com.dmi.cointrader.test

import com.dmi.cointrader.binance.Portfolio
import com.dmi.cointrader.broker.Broker
import com.dmi.cointrader.config.TradeAssets
import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.matchers.shouldThrow
import java.math.BigDecimal
import java.math.RoundingMode

fun Portfolio.round() = mapValues { it.value.setScale(3, RoundingMode.HALF_DOWN) }

class TestExchangeSpec : Spec({
    val assets = TradeAssets(main = "BTC", alts = listOf("LTC", "ETH"))
    val exchange = TestExchange(assets, BigDecimal("0.01"))

    "initial portfolio" {
        exchange.portfolio() shouldBe mapOf("BTC" to BigDecimal.ONE, "LTC" to BigDecimal.ZERO, "ETH" to BigDecimal.ZERO)
    }

    "brokers" {
        exchange.broker("LTC", "BTC", BigDecimal.ZERO, BigDecimal.ZERO) shouldNotBe null
        exchange.broker("ETH", "BTC", BigDecimal.ZERO, BigDecimal.ZERO) shouldNotBe null
        exchange.broker("BTC", "LTC", BigDecimal.ZERO, BigDecimal.ZERO) shouldBe null
        exchange.broker("BTC", "ETH", BigDecimal.ZERO, BigDecimal.ZERO) shouldBe null
        exchange.broker("ETH", "XXX", BigDecimal.ZERO, BigDecimal.ZERO) shouldBe null
        exchange.broker("XXX", "BTC", BigDecimal.ZERO, BigDecimal.ZERO) shouldBe null
    }

    "buy and sell" {
        val broker = exchange.broker("LTC", "BTC", ask = BigDecimal("0.01"), bid = BigDecimal("0.005"))!!

        broker.buy(BigDecimal("40"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.600"),     // 1 - 40 * 0.01
                "LTC" to BigDecimal("39.600"),    // 40 * (1 - 0.01)
                "ETH" to BigDecimal("0.000")
        )

        broker.buy(BigDecimal("20"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.400"),     // 0.600 - 20 * 0.01
                "LTC" to BigDecimal("59.400"),    // 39.600 + 20 * (1 - 0.01)
                "ETH" to BigDecimal("0.000")
        )

        broker.sell(BigDecimal("6"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.430"),     // 0.400 + 6 * 0.005 * (1 - 0.01)
                "LTC" to BigDecimal("53.400"),
                "ETH" to BigDecimal("0.000")
        )

        broker.sell(BigDecimal("53.400"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.694"),     // 0.430 + 53.400 * 0.005 * (1 - 0.01)
                "LTC" to BigDecimal("0.000"),
                "ETH" to BigDecimal("0.000")
        )

        shouldThrow<Broker.OrderError.InsufficientBalance> {
            broker.buy(BigDecimal("1000.0"))
        }

        shouldThrow<Broker.OrderError.InsufficientBalance> {
            broker.sell(BigDecimal("1000.0"))
        }
    }
})