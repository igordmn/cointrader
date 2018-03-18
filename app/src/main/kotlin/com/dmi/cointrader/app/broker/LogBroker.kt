package com.dmi.cointrader.app.broker

import com.dmi.cointrader.app.binance.Asset
import org.slf4j.Logger
import java.math.BigDecimal

fun Broker.log(log: Logger, baseAsset: Asset, quoteAsset: Asset): Broker = LogBroker(this, log, baseAsset, quoteAsset)

class LogBroker(
        private val original: Broker,
        private val log: Logger,
        private val baseAsset: Asset,
        private val quoteAsset: Asset
) : Broker {
    override val limits: Broker.Limits = original.limits

    override suspend fun buy(baseAmount: BigDecimal): Broker.OrderResult {
        log.debug("Buy $baseAmount $baseAsset from $quoteAsset")
        return original.buy(baseAmount).also {
            log.debug("Result $it")
        }
    }

    override suspend fun sell(baseAmount: BigDecimal): Broker.OrderResult {
        log.debug("Sell $baseAmount $baseAsset to $quoteAsset")
        return original.sell(baseAmount).also {
            log.debug("Result $it")
        }
    }
}