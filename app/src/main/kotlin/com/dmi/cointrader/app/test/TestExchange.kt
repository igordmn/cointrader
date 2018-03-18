package com.dmi.cointrader.app.test

import com.dmi.cointrader.app.binance.Asset
import com.dmi.cointrader.app.binance.Portfolio
import com.dmi.cointrader.app.broker.Broker
import com.dmi.cointrader.app.performtrade.TradeAssets
import java.math.BigDecimal

class TestExchange(private val assets: TradeAssets, private val fee: Double) {
    private val portfolio = mapOf(assets.main to BigDecimal.ONE) + assets.alts.associate { it to BigDecimal.ZERO }

    fun portfolio(): Portfolio = portfolio

    fun broker(baseAsset: Asset, quoteAsset: Asset): Broker? {
        return if (baseAsset == assets.main && quoteAsset in assets.alts) {
            object: Broker {
                override val limits = Broker.Limits(BigDecimal.ZERO, BigDecimal.ZERO)

                suspend override fun buy(baseAmount: BigDecimal): Broker.OrderResult {

                }

                suspend override fun sell(baseAmount: BigDecimal): Broker.OrderResult {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }
        } else {
            null
        }
    }
}