package com.dmi.cointrader.app.moment

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.trade.Trade
import com.dmi.util.atom.Atom
import com.dmi.util.atom.MemoryAtom
import com.dmi.util.atom.SyncAtom
import com.dmi.util.collection.SuspendList
import com.dmi.util.test.Spec
import com.dmi.util.test.instant
import com.dmi.util.test.period
import io.kotlintest.matchers.shouldBe
import kotlinx.coroutines.experimental.channels.toList
import java.time.Instant

class TradeMomentsSpec : Spec({
    val startTime = instant(10)
    val period = period(5)
    val currentTime = MemoryAtom(instant(21))
    val ethTrades = TradeList(
            Trade(instant(15), amount = 2.0, price = 3.5),
            Trade(instant(18), amount = 2.0, price = 2.5),
            Trade(instant(21), amount = 2.0, price = 4.5),
            Trade(instant(25), amount = 2.0, price = 1.5),
            Trade(instant(28), amount = 2.0, price = 6.5)
    )
    val ltcTrades = TradeList(
            Trade(instant(15), amount = 2.0, price = 30.5),
            Trade(instant(28), amount = 2.0, price = 60.5)
    )
    val xrpTrades = TradeList(
            Trade(instant(15), amount = 2.0, price = 300.5),
            Trade(instant(15), amount = 2.0, price = 300.5)
    )
    val moments = TradeMoments(startTime, period, listOf(ethTrades, ltcTrades, xrpTrades), currentTime)

    "get moments" {
        moments.restore(null).toList() shouldBe listOf(
                MomentItem(
                        MomentState(0L, listOf(CandleState(), CandleState(), CandleState())),
                        Moment(listOf(Candle(), Candle(), Candle()))
                )
        )
    }
})

private class TradeList(vararg trades: Trade) : SuspendList<Trade> {
    private val trades = trades.toList()

    suspend override fun size(): Long = trades.size.toLong()

    suspend override fun get(range: LongRange): List<Trade> = trades.subList(range.start.toInt(), range.endInclusive.toInt() - 1)
}