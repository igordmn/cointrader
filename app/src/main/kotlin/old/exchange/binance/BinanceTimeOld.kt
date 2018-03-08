package old.exchange.binance

import com.dmi.util.atom.Atom
import com.dmi.util.atom.ReadAtom
import old.exchange.*
import old.exchange.binance.api.BinanceAPI
import java.time.Instant

class BinanceTimeOld(
        private val api: BinanceAPI
) : ExchangeTime {
    override suspend fun current(): Instant {
        val serverTime = api.serverTime()
        return Instant.ofEpochMilli(serverTime.serverTime)
    }
}

class BinanceTime(
        private val api: BinanceAPI
) : ReadAtom<Instant> {
    suspend override fun invoke(): Instant {
        val serverTime = api.serverTime()
        return Instant.ofEpochMilli(serverTime.serverTime)
    }
}