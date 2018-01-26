package exchange

import exchange.candle.Candle
import exchange.candle.TimedCandle
import exchange.history.MarketHistory
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class ReversedMarketHistory(
        private val original: MarketHistory,
        private val operationScale: Int
) : MarketHistory {
    override suspend fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> {
        fun Candle.reverse() = Candle(
                close = BigDecimal.ONE.divide(close, operationScale, RoundingMode.HALF_UP),
                open = BigDecimal.ONE.divide(open, operationScale, RoundingMode.HALF_UP),
                low = BigDecimal.ONE.divide(high, operationScale, RoundingMode.HALF_UP),
                high = BigDecimal.ONE.divide(low, operationScale, RoundingMode.HALF_UP)
        )

        return original.candlesBefore(time).map {
            TimedCandle(it.timeRange, it.item.reverse())
        }
    }
}