package com.dmi.cointrader.app.history

import com.dmi.cointrader.app.binance.BinanceExchange
import com.dmi.cointrader.app.candle.Period
import com.dmi.cointrader.app.moment.Moment
import java.time.Instant

typealias History = List<Moment>

suspend fun binanceArchive(exchange: BinanceExchange): Archive {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates
}

class Archive {
    suspend fun historyAt(period: Period, size: Int): History {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates
    }

    fun load(currentTime: Instant) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}