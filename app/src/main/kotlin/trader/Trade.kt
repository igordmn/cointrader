package trader

import java.time.Instant

interface Trade {
    // TODO it is wrong to pass time
    suspend fun perform(time: Instant)
}

class MultipleTrade(private val trade: List<Trade>): Trade {
    override suspend fun perform(time: Instant) {
        trade.forEach {
            it.perform(time)
        }
    }
}