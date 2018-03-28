package com.dmi.cointrader.broker

import com.dmi.cointrader.test.TestExchange
import com.dmi.cointrader.test.round
import com.dmi.cointrader.trade.TradeAssets
import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import java.math.BigDecimal

class SafeBrokerSpec : Spec({
    val assets = TradeAssets(main = "BTC", alts = listOf("LTC"))
    val exchange = TestExchange(assets, BigDecimal.ZERO)

    fun safeBroker(amountStep: BigDecimal = BigDecimal("0.02")): SafeBroker {
        val limits = Broker.Limits(minAmount = BigDecimal("0.1"), amountStep = amountStep)
        val limitedBroker = object : Broker {
            val original = exchange.broker("BTC", "LTC", BigDecimal("100"), BigDecimal("100"))!!
            override val limits: Broker.Limits = limits
            suspend override fun buy(amount: BigDecimal) = original.buy(amount)
            suspend override fun sell(amount: BigDecimal) = original.sell(amount)
        }
        val attempts = SafeBroker.Attempts(count = 3, amountMultiplier = 0.99)
        return SafeBroker(limitedBroker, attempts)
    }

    "can buy/sell zero amount" {
        val safeBroker = safeBroker()
        safeBroker.buy(BigDecimal("0.0"))
        safeBroker.sell(BigDecimal("0.0"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("1.000"),
                "LTC" to BigDecimal("0.000")
        )
    }

    "can buy amount smaller than minimal" {
        val safeBroker = safeBroker()
        safeBroker.buy(BigDecimal("0.05"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("1.000"),
                "LTC" to BigDecimal("0.000")
        )
    }

    "can sell amount smaller than minimal" {
        val safeBroker = safeBroker()
        safeBroker.sell(BigDecimal("0.05"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("1.000"),
                "LTC" to BigDecimal("0.000")
        )
    }

    "can sell/buy amount not multiply by amount step" {
        val safeBroker = safeBroker()
        safeBroker.sell(BigDecimal("0.15"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.860"),
                "LTC" to BigDecimal("14.000")
        )

        safeBroker.buy(BigDecimal("0.13"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.980"),
                "LTC" to BigDecimal("2.000")
        )
    }

    "decrease amount by 0.99 when first sell unsuccessful with insufficient balance" {
        val safeBroker = safeBroker(amountStep = BigDecimal("0.002"))
        safeBroker.sell(BigDecimal("1.01")) // 1.01 * 0.99 = 0.9999, round(0.9999, 0.002) = 0.998
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.002"),
                "LTC" to BigDecimal("99.800")
        )
    }

    "decrease amount by 0.99 when first and second sell unsuccessful with insufficient balance" {
        val safeBroker = safeBroker(amountStep = BigDecimal("0.002"))
        safeBroker.sell(BigDecimal("1.02")) // 1.02 * 0.99 * 0.99 = 0.999702  round(0.999702, 0.002) = 0.998
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.002"),
                "LTC" to BigDecimal("99.800")
        )
    }

    "fourth attempt not allowed when first, second and third sell unsuccessful with insufficient balance" {
        val safeBroker = safeBroker(amountStep = BigDecimal("0.002"))
        shouldThrow<Broker.OrderError.InsufficientBalance> {
            safeBroker.buy(BigDecimal("1.03")) // 1.03 * 0.99 * 0.99 = 1.009503 > 1.0
        }
    }

    "amount cannot be negative" {
        val safeBroker = safeBroker()
        shouldThrow<Broker.OrderError.WrongAmount> {
            safeBroker.buy(BigDecimal("-1.0"))
        }
        shouldThrow<Broker.OrderError.WrongAmount> {
            safeBroker.sell(BigDecimal("-1.0"))
        }
    }
})