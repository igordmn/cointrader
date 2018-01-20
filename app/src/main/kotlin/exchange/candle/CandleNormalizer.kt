package exchange.candle

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import util.lang.time.PeriodicItems
import util.lang.time.RangeTimedCutter
import java.time.Duration
import java.time.Instant

interface CandleNormalizer {
    fun normalizeBefore(candles: ReceiveChannel<TimedCandle>, endTime: Instant, period: Duration): ReceiveChannel<TimedCandle>
}

fun approximateCandleNormalizer(
        approximatedPricesFactory: ApproximatedPricesFactory
) = object : CandleNormalizer {
    override fun normalizeBefore(candles: ReceiveChannel<TimedCandle>, endTime: Instant, period: Duration): ReceiveChannel<TimedCandle> {
        fun cutInside(candle: Candle, t1: Double, t2: Double): Candle {
            return approximatedPricesFactory.forCandle(candle).cutCandle(t1, t2)
        }

        val skipped = candles.fillSkipped()
        val cutter = RangeTimedCutter(::cutInside)
        val merger = TimedCandleMerger()
        val periodicItems = PeriodicItems(skipped, cutter, merger, period)
        return periodicItems.before(endTime)
    }
}