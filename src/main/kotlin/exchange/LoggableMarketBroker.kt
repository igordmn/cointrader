package exchange

import org.slf4j.Logger
import java.math.BigDecimal

class LoggableMarketBroker(
        private val original: MarketBroker,
        private val fromCoin: String,
        private val toCoin: String,
        private val log: Logger
) : MarketBroker {
    override suspend fun buy(amount: BigDecimal) {
        log.info("buying $amount $toCoin from $fromCoin")
        original.buy(amount)
        log.info("buying successful")
    }

    override suspend fun sell(amount: BigDecimal) {
        log.info("selling $amount $toCoin to $fromCoin")
        original.sell(amount)
        log.info("selling successful")
    }
}