package exchange

import org.slf4j.Logger
import util.math.round
import java.math.BigDecimal

class LoggableMarketBroker(
        private val original: MarketBroker,
        private val fromCoin: String,
        private val toCoin: String,
        private val log: Logger
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) {
        val amountR = amount.round(10)
        log.debug("buying $amountR $toCoin from $fromCoin")
        original.buy(amount)
        log.debug("buying successful")
    }

    override suspend fun sell(amount: BigDecimal) {
        val amountR = amount.round(10)
        log.debug("selling $amountR $toCoin to $fromCoin")
        original.sell(amount)
        log.debug("selling successful")
    }
}