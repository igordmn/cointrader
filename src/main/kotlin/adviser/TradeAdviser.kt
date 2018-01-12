package adviser

import exchange.CoinToCandles
import java.math.BigDecimal

typealias CoinPortions = Map<String, BigDecimal>

interface TradeAdviser {
    fun bestPortfolioPortions(currentPortions: CoinPortions, previousCandles: CoinToCandles): CoinPortions
}