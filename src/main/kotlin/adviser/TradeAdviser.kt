package adviser

import exchange.CoinToCandles
import exchange.MarketHistory
import util.math.Portions
import java.math.BigDecimal

interface TradeAdviser {
    fun bestPortfolioPortions(currentPortions: Portions, previousCandles: CoinToCandles): Portions
}