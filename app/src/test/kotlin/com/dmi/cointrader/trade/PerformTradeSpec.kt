package com.dmi.cointrader.trade

import com.dmi.cointrader.archive.Spread
import com.dmi.cointrader.archive.Spreads
import com.dmi.cointrader.binance.Asset
import com.dmi.cointrader.broker.Broker
import com.dmi.cointrader.config.TradeAssets
import com.dmi.cointrader.neural.Portions
import com.dmi.cointrader.test.TestExchange
import com.dmi.cointrader.test.round
import com.dmi.util.lang.indexOfMax
import com.dmi.util.test.Spec
import io.kotlintest.matchers.shouldBe
import java.math.BigDecimal

class PerformTradeSpec : Spec({
    val assets = TradeAssets(main = "BTC", alts = listOf("ETH", "LTC"))
    val exchange = TestExchange(assets, BigDecimal.ZERO)

    val spreads = listOf(
            listOf(Spread(0.2, 0.1), Spread(0.05, 0.03)),
            listOf(Spread(0.3, 0.2), Spread(0.06, 0.03)),
            listOf(Spread(0.4, 0.2), Spread(0.07, 0.05)),
            listOf(Spread(0.4, 0.3), Spread(0.04, 0.03))
    )

    fun broker(baseAsset: Asset, quoteAsset: Asset, spread: Spread) = exchange.broker(
            baseAsset, quoteAsset,
            spread.ask.toBigDecimal(),
            spread.bid.toBigDecimal()
    )

    "performTrades" {
        fun getBestPortions(current: Portions) = when {
            current.indexOfMax() == 0 -> listOf(0.0, 1.0, 0.0)
            current.indexOfMax() == 1 -> listOf(0.0, 0.0, 1.0)
            else -> listOf(1.0, 0.0, 0.0)
        }

        val performTestTrade = object {
            suspend operator fun invoke(spreads: Spreads) {
                fun broker(baseAsset: Asset, quoteAsset: Asset): Broker? {
                    val index = assets.alts.indexOf(baseAsset)
                    return if (index >= 0) broker(baseAsset, quoteAsset, spreads[index]) else null
                }
                performTrade(assets, exchange.portfolio(), spreads, ::getBestPortions, ::broker)
            }
        }

        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("1.000"),
                "ETH" to BigDecimal("0.000"),
                "LTC" to BigDecimal("0.000")
        )

        performTestTrade(spreads[0])
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.000"),
                "ETH" to BigDecimal("5.000"),   // 1.000 / 0.2
                "LTC" to BigDecimal("0.000")
        )

        performTestTrade(spreads[1])
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.010"),
                "ETH" to BigDecimal("0.000"),
                "LTC" to BigDecimal("16.500")   // 5.000 * 0.99 * 0.2 / 0.06
        )

        performTestTrade(spreads[2])
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.835"),   // 0.010 + 16.500 * 0.05
                "ETH" to BigDecimal("0.000"),
                "LTC" to BigDecimal("0.000")
        )


        performTestTrade(spreads[2])
        exchange.portfolio().round() shouldBe mapOf(
                "BTC" to BigDecimal("0.000"),
                "ETH" to BigDecimal("2.087"),   // 0.835 / 0.4
                "LTC" to BigDecimal("0.000")
        )
    }
})