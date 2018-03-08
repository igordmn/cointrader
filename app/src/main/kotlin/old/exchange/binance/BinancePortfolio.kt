package old.exchange.binance

import old.exchange.Portfolio
import com.dmi.cointrader.app.binance.api.BinanceAPI
import java.math.BigDecimal
import java.time.Instant

class BinancePortfolio(
        private val constants: BinanceConstants,
        private val api: BinanceAPI
) : Portfolio {
    override suspend fun amounts(): Map<String, BigDecimal> {
        // todo брать время с сервера
        val result = api.getAccount(5000, Instant.now().toEpochMilli())
        return result.balances.associate {
            val standardName = constants.binanceNameToStandard[it.asset] ?: it.asset
            standardName to BigDecimal(it.free)
        }
    }
}