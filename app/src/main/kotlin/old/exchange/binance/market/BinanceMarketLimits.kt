package old.exchange.binance.market

import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.api.client.domain.general.FilterType
import old.exchange.MarketLimits
import java.math.BigDecimal

class BinanceMarketLimits(
        private val name: String,
        private val exchangeInfo: ExchangeInfo
) : MarketLimits {
    override fun get(): MarketLimits.Value {
        val symbolInfo = exchangeInfo.symbols.find { it.symbol == name }!!
        val lotSizeFilter = symbolInfo.filters.find { it.filterType == FilterType.LOT_SIZE }!!
        return MarketLimits.Value(
                minAmount = BigDecimal(lotSizeFilter.minQty),
                amountStep = BigDecimal(lotSizeFilter.stepSize)
        )
    }
}