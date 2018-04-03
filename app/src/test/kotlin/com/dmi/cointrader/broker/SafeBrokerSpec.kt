package com.dmi.cointrader.broker

import com.dmi.cointrader.test.TestExchange
import com.dmi.cointrader.test.round
import com.dmi.cointrader.config.TradeAssets
import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import java.math.BigDecimal

class SafeBrokerSpec : Spec({
    val assets = TradeAssets(main = "BTC", alts = listOf("LTC"))
    val exchange = TestExchange(assets, BigDecimal.ZERO)

    fun safeBroker(amountStep: BigDecimal = BigDecimal("0.2")): SafeBroker {
        val limits = Broker.Limits(minAmount = BigDecimal("0.1"), amountStep = amountStep)
        val limitedBroker = object : Broker {
            val original = exchange.broker("LTC", "BTC", BigDecimal("0.01"), BigDecimal("0.01"))!!
            override val limits: Broker.Limits = limits
            override suspend fun buy(amount: BigDecimal) = original.buy(amount)
            override suspend fun sell(amount: BigDecimal) = original.sell(amount)
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

    "can buy/sell amount not multiply by amount step" {
        val safeBroker = safeBroker()
        safeBroker.buy(BigDecimal("10.1"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.900"),
                "LTC" to BigDecimal("10.000")
        )

        safeBroker.sell(BigDecimal("9.9"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.998"),
                "LTC" to BigDecimal("0.200")
        )
    }

    "decrease amount by 0.99 when first buy unsuccessful with insufficient balance" {
        val safeBroker = safeBroker()
        safeBroker.buy(BigDecimal("100.2")) // 100.2 * 0.99 = 99.198, round(99.198, 0.2) = 99
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.010"),
                "LTC" to BigDecimal("99.000")
        )
    }

    "decrease amount by 0.99 when first and second buy unsuccessful with insufficient balance" {
        val safeBroker = safeBroker()
        safeBroker.buy(BigDecimal("101.5")) // 101.5 * 0.99 * 0.99 = 99.48015,  round(99.48015, 0.2) = 99.4
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.006"),
                "LTC" to BigDecimal("99.400")
        )
    }

    "fourth attempt not allowed when first, second and third buy unsuccessful with insufficient balance" {
        val safeBroker = safeBroker()
        shouldThrow<Broker.OrderError.InsufficientBalance> {
            safeBroker.buy(BigDecimal("102.3")) // 102.3 * 0.99 * 0.99 = 100.26423, round(100.26423, 0.2) = 100.2 > 100
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