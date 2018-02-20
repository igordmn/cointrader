package data

import com.binance.api.client.domain.market.AggTrade
import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.nio.file.Path
import java.sql.Blob
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant

class TradeCache private constructor(private val connection: Connection) : AutoCloseable {
    private val thread = newSingleThreadContext("historyCache")

    private suspend fun modify(action: suspend () -> Unit) {
        async(thread) {
            try {
                action()
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            }
        }.await()
    }

    private suspend fun <T> query(action: suspend () -> T): T {
        return async(thread) {
            action()
        }.await()
    }

    private suspend fun update() = modify {
//        TODO
    }

    suspend fun lastTradeId(market: String): Long? = query {
        connection.prepareStatement("SELECT max(lastTradeId) FROM TradeChunk WHERE market=?").use {
            it.setString(1, market)
            it.executeQuery().use { rs ->
                val hasRows = rs.next()
                if (hasRows) {
                    rs.getLong(1)
                } else {
                    null
                }
            }
        }
    }

    suspend fun insertTrades(
            market: String,
            trades: List<AggTrade>
    ) {
        require(trades.isNotEmpty())

        modify {
            connection.prepareStatement("INSERT INTO TradeChunk VALUES (?,?,?,?)").use {
                it.setString(1, market)
                it.setLong(2, trades.last().aggregatedTradeId)
                it.setInt(3, trades.size)
                it.setBytes(4, toBytes(trades))

                it.executeUpdate()
            }
        }
    }

    private fun toBytes(trades: List<AggTrade>): ByteArray {
        val bs = ByteArrayOutputStream()
        ObjectOutputStream(bs).use { os ->
            trades.forEach {
                os.writeLong(it.tradeTime)
                os.writeDouble(it.quantity.toDouble())
                os.writeDouble(it.price.toDouble())
            }
        }
        return bs.toByteArray()
    }

    override fun close() = connection.close()

    companion object {
        suspend fun create(path: Path): TradeCache {
            Class.forName("org.sqlite.JDBC")
            val connection = DriverManager.getConnection("jdbc:sqlite:$path")
            connection.autoCommit = false
            return TradeCache(connection).apply {
                update()
            }
        }
    }
}