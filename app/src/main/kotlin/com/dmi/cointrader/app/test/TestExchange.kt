package com.dmi.cointrader.app.test

import com.dmi.cointrader.app.binance.Asset
import com.dmi.cointrader.app.binance.Portfolio
import com.dmi.cointrader.app.broker.Broker
import com.dmi.cointrader.app.performtrade.TradeAssets
import java.math.BigDecimal

class TestExchange(private val assets: TradeAssets, private val fee: BigDecimal) {
    private val portfolio = HashMap(mapOf(assets.main to BigDecimal.ONE) + assets.alts.associate { it to BigDecimal.ZERO })

    fun portfolio(): Portfolio = HashMap(portfolio)

    fun broker(baseAsset: Asset, quoteAsset: Asset, quotePrice: BigDecimal): Broker? {
        return if (baseAsset == assets.main && quoteAsset in assets.alts) {
            object: Broker {
                override val limits = Broker.Limits(BigDecimal.ZERO, BigDecimal.ZERO)

                suspend override fun buy(baseAmount: BigDecimal): Broker.OrderResult = synchronized(portfolio) {
                    val currentBaseAmount = portfolio[baseAsset]!!
                    val currentQuoteAmount = portfolio[quoteAsset]!!
                    val quoteAmount = baseAmount * quotePrice
                    if (quoteAmount > currentQuoteAmount) {
                        throw Broker.OrderError.InsufficientBalance
                    }
                    portfolio[baseAsset] = currentBaseAmount + baseAmount * (BigDecimal.ONE - fee)
                    portfolio[quoteAsset] = currentQuoteAmount - quoteAmount
                    return Broker.OrderResult(1.0)
                }

                suspend override fun sell(baseAmount: BigDecimal): Broker.OrderResult = synchronized(portfolio) {
                    val currentBaseAmount = portfolio[baseAsset]!!
                    val currentQuoteAmount = portfolio[quoteAsset]!!
                    val quoteAmount = baseAmount * quotePrice
                    if (baseAmount > currentBaseAmount) {
                        throw Broker.OrderError.InsufficientBalance
                    }
                    portfolio[baseAsset] = currentBaseAmount - baseAmount
                    portfolio[quoteAsset] = currentQuoteAmount + quoteAmount * (BigDecimal.ONE - fee)
                    return Broker.OrderResult(1.0)
                }
            }
        } else {
            null
        }
    }
}