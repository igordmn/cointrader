package data

import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import java.nio.file.Path
import java.sql.Connection
import java.time.Instant
import org.h2.jdbcx.JdbcConnectionPool
import java.sql.Timestamp


class HistoryCache private constructor(path: Path) : AutoCloseable {
    private val connectionPool = JdbcConnectionPool.create(jdbcPath(path), "", "").apply {
        maxConnections = 100
    }

    private fun jdbcPath(path: Path) = "jdbc:h2:${path.toAbsolutePath()}"

    private suspend fun modify(action: suspend (connection: Connection) -> Unit) {
        connect().use {
            try {
                action(it)
            } catch (e: Throwable) {
                it.rollback()
                throw e
            }
            it.commit()
        }
    }

    private fun connect(): Connection {
        val connection = connectionPool.connection
        connection.autoCommit = false
        return connection
    }

    private suspend fun update() = modify {
        it.createStatement().use {
            it.executeUpdate("""
                CREATE TABLE IF NOT EXISTS HistoryCandle(
                    market VARCHAR(20) NOT NULL,
                    openTime TIMESTAMP NOT NULL,
                    closeTime TIMESTAMP NOT NULL,
                    open DECIMAL(40,20) NOT NULL,
                    close DECIMAL(40,20) NOT NULL,
                    high DECIMAL(40,20) NOT NULL,
                    low DECIMAL(40,20) NOT NULL,
                    PRIMARY KEY (market, closeTime)
                )
            """.trimIndent()
            )
        }
    }

    suspend fun insertCandles(
            market: String,
            candles: ReceiveChannel<TimedCandle>
    ) = modify {
        candles.consumeEach { candle ->
            it.prepareStatement("INSERT INTO HistoryCandle VALUES (?,?,?,?,?,?,?)").use {
                it.setString(1, market)
                it.setTimestamp(2, Timestamp.from(candle.timeRange.start))
                it.setTimestamp(3, Timestamp.from(candle.timeRange.endInclusive))
                it.setBigDecimal(4, candle.item.open)
                it.setBigDecimal(5, candle.item.close)
                it.setBigDecimal(6, candle.item.high)
                it.setBigDecimal(7, candle.item.low)
                it.executeUpdate()
            }
        }
    }

    fun lastCloseTime(
            market: String
    ): Instant = connect().use {
        it.prepareStatement("SELECT max(closeTime) FROM HistoryCandle WHERE market=?").use {
            it.setString(1, market)
            it.executeQuery().use { rs ->
                val hasRows = rs.next()
                return if (hasRows) {
                    val timestamp = rs.getTimestamp(1)
                    timestamp?.toInstant() ?: Instant.MIN
                } else {
                    Instant.MIN
                }
            }
        }
    }

    fun candlesBefore(
            market: String,
            time: Instant
    ): ReceiveChannel<TimedCandle> = produce {
        connect().use {
            it.prepareStatement(
                    """
                        SELECT openTime, closeTime, open, close, high, low
                        FROM HistoryCandle
                        WHERE market=? and closeTime<=?
                        ORDER BY closeTime DESC
                    """.trimIndent()
            ).use {
                it.setString(1, market)
                it.setTimestamp(2, Timestamp.from(time))
                it.executeQuery().use { rs ->
                    while (rs.next()) {
                        val openTime = rs.getTimestamp(1).toInstant()
                        val closeTime = rs.getTimestamp(2).toInstant()
                        val open = rs.getBigDecimal(3)
                        val close = rs.getBigDecimal(4)
                        val high = rs.getBigDecimal(5)
                        val low = rs.getBigDecimal(6)
                        send(TimedCandle(
                                openTime..closeTime,
                                Candle(open, close, high, low)
                        ))
                    }
                }
            }
        }
    }

    override fun close() = Unit

    companion object {
        suspend fun create(path: Path) = HistoryCache(path).apply {
            Class.forName("org.h2.Driver").newInstance()
            update()
        }
    }
}