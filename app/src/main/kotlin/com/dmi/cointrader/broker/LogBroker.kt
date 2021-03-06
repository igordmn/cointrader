package com.dmi.cointrader.broker

import com.dmi.cointrader.binance.Asset
import com.dmi.util.io.appendLine
import org.slf4j.Logger
import java.math.BigDecimal
import java.nio.file.Path
import java.time.Instant

fun Broker.log(log: Logger, baseAsset: Asset, quoteAsset: Asset): Broker = LogBroker(this, log, baseAsset, quoteAsset)

class LogBroker(
        private val original: Broker,
        private val log: Logger,
        private val baseAsset: Asset,
        private val quoteAsset: Asset
) : Broker {
    override val limits: Broker.Limits = original.limits

    override suspend fun buy(amount: BigDecimal): Broker.OrderResult {
        log.debug("Buy $amount $baseAsset from $quoteAsset")
        return original.buy(amount).also {
            log.debug("Result $it")
        }
    }

    override suspend fun sell(amount: BigDecimal): Broker.OrderResult {
        log.debug("Sell $amount $baseAsset to $quoteAsset")
        return original.sell(amount).also {
            log.debug("Result $it")
        }
    }
}