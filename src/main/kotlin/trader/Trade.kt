package trader

import java.time.Instant

interface Trade {
    // TODO it is wrong to pass time
    suspend fun perform(time: Instant)
}