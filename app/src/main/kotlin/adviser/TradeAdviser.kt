package adviser

import old.exchange.candle.CoinToCandles
import java.math.BigDecimal

typealias CoinPortions = Map<String, BigDecimal>

interface TradeAdviser {
    suspend fun bestPortfolioPortions(currentPortions: CoinPortions, previousCandles: CoinToCandles): CoinPortions
}