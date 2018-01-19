package exchange.binance

import exchange.Portfolio
import exchange.binance.api.BinanceAPI
import exchange.binance.api.DEFAULT_RECEIVING_WINDOW
import java.math.BigDecimal
import java.time.Instant

class BinancePortfolio(
        private val info: BinanceInfo,
        private val api: BinanceAPI
) : Portfolio {
    override suspend fun amounts(): Map<String, BigDecimal> {
        val result = api.getAccount(DEFAULT_RECEIVING_WINDOW, Instant.now().toEpochMilli())
        return result.balances.associate {
            val standardName = info.binanceNameToStandard[it.asset] ?: it.asset
            standardName to BigDecimal(it.free)
        }
    }
}