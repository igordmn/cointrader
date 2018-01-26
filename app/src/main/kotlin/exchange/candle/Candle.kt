package exchange.candle

import kotlinx.serialization.*
import util.lang.RangeTimed
import util.lang.RangeTimedMerger
import util.math.max
import util.math.min
import java.math.BigDecimal

typealias CoinToCandles = Map<String, List<Candle>>

@Serializable
data class Candle(
        val open: BigDecimal,
        val close: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal
)  {
    init {
        require(high >= open)
        require(high >= close)
        require(low <= open)
        require(low <= close)
        require(high >= low)
    }
}

typealias TimedCandle = RangeTimed<Candle>

class TimedCandleMerger : RangeTimedMerger<Candle> {
    override fun merge(a: TimedCandle, b: TimedCandle): TimedCandle {
        require(a.timeRange.endInclusive == b.timeRange.start)
        return RangeTimed(
                timeRange = a.timeRange.start..b.timeRange.endInclusive,
                item = Candle(
                        open = a.item.open,
                        close = b.item.close,
                        high = max(a.item.high, b.item.high),
                        low = min(a.item.low, b.item.low)
                )
        )
    }
}