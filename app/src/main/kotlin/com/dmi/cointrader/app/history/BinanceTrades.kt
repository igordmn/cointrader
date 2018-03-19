package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.util.concurrent.buildChannel
import com.dmi.util.concurrent.map
import com.dmi.util.io.RestorableSource
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.takeWhile
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
class BinanceTradeState(val id: Long)

typealias BinanceTradeItem = RestorableSource.Item<BinanceTradeState, Trade>

@Serializable
data class BinanceTradeConfig(val market: String)

class BinanceTrades(
        private val market: BinanceExchange.Market,
        private var currentTime: Instant,
        private val chunkLoadCount: Int
) : RestorableSource<BinanceTradeState, Trade> {
    override fun restore(state: BinanceTradeState?): ReceiveChannel<BinanceTradeItem> = buildChannel {
        val startId = if (state != null) state.id + 1L else 0L
        market.trades(startId, chunkLoadCount).map(::convert).takeWhile { it.value.time <= currentTime }
    }

    private fun convert(trade: BinanceExchange.Trade) = BinanceTradeItem(
            BinanceTradeState(trade.aggTradeId),
            Trade(
                    trade.time,
                    trade.amount.toDouble(),
                    trade.price.toDouble()
            )
    )
}