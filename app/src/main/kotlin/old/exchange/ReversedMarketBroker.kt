package old.exchange

import java.math.BigDecimal
import java.math.RoundingMode

class ReversedMarketBroker(
        private val original: MarketBroker,
        private val price: BigDecimal,
        private val operationScale: Int
): MarketBroker {
    override suspend fun buy(amount: BigDecimal) = original.sell(amount.divide(price, operationScale, RoundingMode.HALF_UP))
    override suspend fun sell(amount: BigDecimal) = original.buy(amount.divide(price, operationScale, RoundingMode.HALF_UP))
}