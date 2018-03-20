package com.dmi.cointrader.app.candle

import com.dmi.cointrader.app.archive.IndexedTrade
import com.dmi.util.concurrent.chunkedBy
import com.dmi.util.concurrent.insert
import com.dmi.util.concurrent.map
import com.dmi.util.lang.DurationSerializer
import com.dmi.util.lang.InstantSerializer
import com.dmi.util.lang.MILLIS_PER_DAY
import com.dmi.util.lang.times
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

data class TradesCandle<out TRADE_INDEX>(val num: Long, val candle: Candle, val lastTradeIndex: TRADE_INDEX)

data class Period(val num: Long) : Comparable<Period> {
    override fun compareTo(other: Period): Int = num.compareTo(other.num)
    fun next(count: Int = 1): Period = Period(num + count)
    fun previous(count: Int = 1): Period = Period(num - count)
    infix fun until(end: Period): PeriodRange = this..Period(end.num - 1)
}

@Serializable
data class Periods(
        @Serializable(with = InstantSerializer::class) val start: Instant,
        @Serializable(with = DurationSerializer::class) val duration: Duration
) {
    fun of(time: Instant) = Period(numOf(time))

    private fun numOf(time: Instant) : Long {
        val distMillis = Duration.between(start, time).toMillis()
        val periodMillis = duration.toMillis()
        return Math.floorDiv(distMillis, periodMillis)
    }

    fun startOf(period: Period): Instant {
        return start + duration * period.num
    }

    fun perDay(): Double = MILLIS_PER_DAY / duration.toMillis().toDouble()
}

typealias PeriodRange = ClosedRange<Period>
fun PeriodRange.nums(): LongRange = start.num..endInclusive.num
fun PeriodRange.asSequence(): Sequence<Period> = (start.num..endInclusive.num).asSequence().map(::Period)
fun PeriodRange.size() = endInclusive.num - start.num + 1
fun LongRange.toPeriods(): PeriodRange = Period(start)..Period(endInclusive)

fun <INDEX> ReceiveChannel<IndexedTrade<INDEX>>.candles(
        periods: Periods,
        nums: LongRange
): ReceiveChannel<TradesCandle<INDEX>> {
    class CandleBillet(val num: Long, val trades: List<IndexedTrade<INDEX>>) {
        fun build() = TradesCandle(num, Candle(
                trades.last().value.price,
                trades.maxBy { it.value.price }!!.value.price,
                trades.minBy { it.value.price }!!.value.price
        ), trades.last().index)
    }

    fun candleNum(trade: IndexedTrade<INDEX>) = periods.of(trade.value.time).num

    fun candlesFirst(first: CandleBillet): List<CandleBillet> = (nums.start until first.num).map {
        CandleBillet(it, listOf(first.trades.first()))
    }

    fun candlesLast(last: CandleBillet): List<CandleBillet> = (last.num + 1..nums.endInclusive).map {
        CandleBillet(it, listOf(last.trades.last()))
    }

    fun candlesBetween(previous: CandleBillet, next: CandleBillet) = (previous.num + 1 until next.num).map {
        CandleBillet(it, listOf(previous.trades.last()))
    }

    return this
            .chunkedBy(::candleNum, ::CandleBillet)
            .insert(::candlesFirst, ::candlesBetween, ::candlesLast)
            .dropWhile { it.num < nums.start }
            .takeWhile { it.num <= nums.endInclusive }
            .map(CandleBillet::build)
}