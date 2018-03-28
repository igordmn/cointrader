package com.dmi.cointrader.archive

import com.dmi.cointrader.binance.BinanceExchange
import com.dmi.util.concurrent.emptyChannel
import com.dmi.util.concurrent.map
import com.dmi.util.restorable.RestorableSource
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
class TradeState(val id: Long, val time: Instant)

class TradeSource(
        private val market: BinanceExchange.Market,
        private val currentTime: Instant,
        private val chunkLoadCount: Int
) : RestorableSource<TradeState, Trade> {
    override fun initial() = trades(0)
    override fun restored(state: TradeState) = if (currentTime > state.time) trades(state.id + 1L) else emptyChannel()

    private fun trades(startId: Long) = market
            .trades(startId, chunkLoadCount)
            .map(::convert)
            .takeWhile { it.value.time <= currentTime }

    private fun convert(trade: BinanceExchange.Trade) = RestorableSource.Item(
            TradeState(trade.aggTradeId, trade.time),
            Trade(
                    trade.time,
                    trade.price.toDouble(),
                    trade.amount.toDouble()
            )
    )
}