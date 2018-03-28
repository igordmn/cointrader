package com.dmi.cointrader.test

import com.dmi.cointrader.binance.Portfolio
import com.dmi.cointrader.broker.Broker
import com.dmi.cointrader.trade.TradeAssets
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
        exchange.broker("BTC", "LTC", BigDecimal.ZERO, BigDecimal.ZERO) shouldNotBe null
        exchange.broker("BTC", "ETH", BigDecimal.ZERO, BigDecimal.ZERO) shouldNotBe null
        exchange.broker("LTC", "BTC", BigDecimal.ZERO, BigDecimal.ZERO) shouldBe null
        exchange.broker("ETH", "BTC", BigDecimal.ZERO, BigDecimal.ZERO) shouldBe null
        exchange.broker("ETH", "XXX", BigDecimal.ZERO, BigDecimal.ZERO) shouldBe null
        exchange.broker("XXX", "BTC", BigDecimal.ZERO, BigDecimal.ZERO) shouldBe null
    }

    "sell and buy" {
        val broker = exchange.broker("BTC", "LTC", ask = BigDecimal("150"), bid = BigDecimal("100"))!!

        broker.sell(BigDecimal("0.4"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.600"),
                "LTC" to BigDecimal("39.600"),    // 0.0 + 0.4 * 100 * (1 - 0.01)
                "ETH" to BigDecimal("0.000")
        )

        broker.sell(BigDecimal("0.2"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.400"),
                "LTC" to BigDecimal("59.400"),    // 39.60 + 0.2 * 100 * (1 - 0.01)
                "ETH" to BigDecimal("0.000")
        )

        broker.buy(BigDecimal("0.3"))
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.697"),     // 0.40 + 0.3 * (1 - 0.01)
                "LTC" to BigDecimal("14.400"),    // 59.40 - 0.3 * 150
                "ETH" to BigDecimal("0.000")
        )

        shouldThrow<Broker.OrderError.InsufficientBalance> {
            broker.buy(BigDecimal("1.0"))
        }

        shouldThrow<Broker.OrderError.InsufficientBalance> {
            broker.sell(BigDecimal("1.0"))
        }
    }
})