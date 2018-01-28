package data

import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

class HistoryCache private constructor(private val path: Path) {
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
        Class.forName("org.sqlite.JDBC")
        val connection = DriverManager.getConnection("jdbc:sqlite:$path")
        connection.autoCommit = false
        return connection
    }

    private suspend fun update() = modify {
        it.createStatement().use {
            it.executeUpdate("""
                CREATE TABLE IF NOT EXISTS HistoryCandle(
                    market varchar(20),
                    openTime INTEGER,
                    closeTime INTEGER,
                    open DECIMAL(40,20),
                    close DECIMAL(40,20),
                    high DECIMAL(40,20),
                    low DECIMAL(40,20),
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
                it.setLong(2, candle.timeRange.start.toEpochMilli())
                it.setLong(3, candle.timeRange.endInclusive.toEpochMilli())
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
                    val millis = rs.getLong(1)
                    Instant.ofEpochMilli(millis)
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
                it.setLong(2, time.toEpochMilli())
                it.executeQuery().use { rs ->
                    while (rs.next()) {
                        val openTimeMillis = rs.getLong(1)
                        val closeTimeMillis = rs.getLong(2)
                        val open = rs.getBigDecimal(3)
                        val close = rs.getBigDecimal(4)
                        val high = rs.getBigDecimal(5)
                        val low = rs.getBigDecimal(6)
                        send(TimedCandle(
                                Instant.ofEpochMilli(openTimeMillis)..Instant.ofEpochMilli(closeTimeMillis),
                                Candle(open, close, high, low)
                        ))
                    }
                }
            }
        }
    }

    companion object {
        suspend fun create(path: Path) = HistoryCache(path).apply {
            update()
        }
    }
}