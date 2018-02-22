package com.dmi.cointrader.trade

import com.binance.api.client.domain.market.AggTrade
import exchange.binance.api.BinanceAPI
import kotlinx.coroutines.experimental.channels.*
import java.time.Instant

fun binanceTrades(
        api: BinanceAPI,
        market: String,
        startId: Long,
        downloadTicks: ReceiveChannel<Instant>
): ReceiveChannel<BinanceTrade> = produce {
    var id = startId
    downloadTicks.consumeEach { tick ->
        binanceTrades(api, market, id).takeWhile { it.trade.time <= tick }.consumeEach {
            send(it)
            id = it.aggregatedId + 1
        }
    }
}

private fun binanceTrades(
        api: BinanceAPI,
        market: String,
        startId: Long
): ReceiveChannel<BinanceTrade> = produce {
    val count = 500
    var id = startId

    while(true) {
        val trades = api.getAggTrades(market, id.toString(), count, null, null)
        if (trades.isNotEmpty()) {
            trades.forEach {
                send(it.toBinanceTrade())
            }
            id = trades.last().aggregatedTradeId + 1
        } else {
            break
        }
    }
}

fun AggTrade.toBinanceTrade() = BinanceTrade(
        aggregatedTradeId,
        Trade(
                Instant.ofEpochMilli(tradeTime),
                quantity.toDouble(),
                price.toDouble()
        )
)