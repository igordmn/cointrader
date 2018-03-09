package old.exchange

import org.slf4j.Logger
import com.dmi.util.math.round
import java.math.BigDecimal

class LoggableMarketBroker(
        private val original: Market,
        private val fromCoin: String,
        private val toCoin: String,
        private val log: Logger
) : Market {
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