package com.dmi.cointrader.app.archive

import com.dmi.cointrader.app.candle.Candle
import com.dmi.cointrader.app.candle.Periods
import com.dmi.util.restorable.RestorableSource
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.serialization.Serializable
import java.time.Duration

@Serializable
data class CandlesState<out TRADE_STATE>(val lastTrade: TRADE_STATE)

fun <TRADE_STATE> RestorableSource<TRADE_STATE, TimeSpread>.candles(
        periods: Periods,
        tradeDelay: Duration
) = object : RestorableSource<CandlesState<TRADE_STATE>, Candle> {
    override fun initial(): ReceiveChannel<RestorableSource.Item<CandlesState<TRADE_STATE>, Candle>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun restored(state: CandlesState<TRADE_STATE>): ReceiveChannel<RestorableSource.Item<CandlesState<TRADE_STATE>, Candle>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}