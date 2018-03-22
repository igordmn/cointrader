package com.dmi.cointrader.app.archive

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.util.concurrent.map
import com.dmi.util.restorable.RestorableSource
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
class BinanceTradeState(val id: Long)

typealias BinanceTradeItem = RestorableSource.Item<BinanceTradeState, Trade>

class BinanceTrades(
        private val market: BinanceExchange.Market,
        private val currentTime: Instant,
        private val chunkLoadCount: Int
) : RestorableSource<BinanceTradeState, Trade> {
    override fun initial() = trades(0)
    override fun restored(state: BinanceTradeState) = trades(state.id + 1L)

    private fun trades(startId: Long) = market
            .trades(startId, chunkLoadCount)
            .map(::convert)
            .takeWhile { it.value.time <= currentTime }

    private fun convert(trade: BinanceExchange.Trade) = BinanceTradeItem(
            BinanceTradeState(trade.aggTradeId),
            Trade(
                    trade.time,
                    trade.amount.toDouble(),
                    trade.price.toDouble()
            )
    )
}