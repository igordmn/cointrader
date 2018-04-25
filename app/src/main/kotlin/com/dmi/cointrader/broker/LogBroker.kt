package com.dmi.cointrader.broker

import com.dmi.cointrader.binance.Asset
import com.dmi.util.io.appendLine
import org.slf4j.Logger
import java.math.BigDecimal
import java.nio.file.Path
import java.time.Instant

fun Broker.log(log: Logger, baseAsset: Asset, quoteAsset: Asset): Broker = LogBroker(this, log, baseAsset, quoteAsset)
fun Broker.slippageLog(file: Path, baseAsset: Asset): Broker = SlippageLogBroker(this, file, baseAsset)

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

class SlippageLogBroker(
        private val original: Broker,
        private val file: Path,
        private val baseAsset: Asset
) : Broker {
    override val limits: Broker.Limits = original.limits

    override suspend fun buy(amount: BigDecimal): Broker.OrderResult {
        return original.buy(amount).also {
            val slippage = it.slippage
            val time = Instant.now()
            file.appendLine("$time\tbuy $baseAsset $slippage")
        }
    }

    override suspend fun sell(amount: BigDecimal): Broker.OrderResult {
        return original.sell(amount).also {
            val slippage = it.slippage
            val time = Instant.now()
            file.appendLine("$time\tsell $baseAsset $slippage")
        }
    }
}