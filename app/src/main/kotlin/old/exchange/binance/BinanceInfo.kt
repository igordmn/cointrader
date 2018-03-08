package old.exchange.binance

import com.binance.api.client.domain.general.ExchangeInfo
import old.exchange.MarketLimits
import old.exchange.binance.api.BinanceAPI
import old.exchange.binance.market.BinanceMarketLimits

class BinanceInfo private constructor(
        private val api: BinanceAPI
) {
    private lateinit var exchangeInfo: ExchangeInfo

    fun limits(name: String): MarketLimits = BinanceMarketLimits(name, exchangeInfo)

    suspend fun refresh() {
        exchangeInfo = api.exchangeInfo()
    }

    companion object {
        suspend fun load(api: BinanceAPI): BinanceInfo {
            val binanceInfo = BinanceInfo(api)
            binanceInfo.refresh()
            return binanceInfo
        }
    }
}