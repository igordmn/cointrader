package data

import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.newSingleThreadContext
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant

class HistoryCache private constructor(private val connection: Connection) : AutoCloseable {
    private val thread = newSingleThreadContext("historyCache")

    private suspend fun modify(action: suspend () -> Unit) {
        async(thread) {
            try {
                action()
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
            }
        }.await()
    }

    private suspend fun <T> query(action: suspend () -> T): T {
        return async(thread) {
            action()
        }.await()
    }

    private suspend fun update() = modify {
        connection.createStatement().use {
            it.executeUpdate("""
                CREATE TABLE IF NOT EXISTS HistoryCandle(
                    market VARCHAR(20) NOT NULL,
                    openTime TIMESTAMP NOT NULL,
                    closeTime TIMESTAMP NOT NULL,
                    open DECIMAL(40,20) NOT NULL,
                    close DECIMAL(40,20) NOT NULL,
                    high DECIMAL(40,20) NOT NULL,
                    low DECIMAL(40,20) NOT NULL,
                    PRIMARY KEY (market, openTime, closeTime)
                );
                CREATE UNIQUE INDEX IF NOT EXISTS HistoryCandle_market_closeTime ON HistoryCandle(market, closeTime);
                CREATE UNIQUE INDEX IF NOT EXISTS HistoryCandle_market_openTime ON HistoryCandle(market, openTime);
            """.trimIndent()
            )
        }
    }

    suspend fun lastCloseTimeOf(market: String): Instant? = query {
        connection.prepareStatement("SELECT max(closeTime) FROM HistoryCandle WHERE market=?").use {
            it.setString(1, market)
            it.executeQuery().use { rs ->
                val hasRows = rs.next()
                if (hasRows) {
                    val timestamp = rs.getTimestamp(1)
                    timestamp?.toInstant()
                } else {
                    null
                }
            }
        }
    }

    // todo при вставке, база блокируется, пока все свечи не будет добавлены в базу. но в тоже время в candles записываются новые свечи из сети, и все они хранятся в памяти
    suspend fun insertCandles(
            market: String,
            candles: ReceiveChannel<TimedCandle>
    ) {
        modify {
            connection.prepareStatement("INSERT INTO HistoryCandle VALUES (?,?,?,?,?,?,?)").use {
                candles.consumeEach { candle ->
                    it.setString(1, market)
                    it.setTimestamp(2, candle.timeRange.start.toSqliteTimestamp())
                    it.setTimestamp(3, candle.timeRange.endInclusive.toSqliteTimestamp())
                    it.setBigDecimal(4, candle.item.open)
                    it.setBigDecimal(5, candle.item.close)
                    it.setBigDecimal(6, candle.item.high)
                    it.setBigDecimal(7, candle.item.low)
                    it.addBatch()
                }
                it.executeBatch()
            }
        }
    }

    fun candlesBefore(
            market: String,
            time: Instant
    ): ReceiveChannel<TimedCandle> = produce {
        var chunk = candlesChunkBefore(market, time)
        while (chunk.isNotEmpty()) {
            chunk.forEach {
                send(it)
            }
            chunk = candlesChunkBefore(market, chunk.last().timeRange.start)
        }
    }

    private suspend fun candlesChunkBefore(market: String, time: Instant): List<TimedCandle> {
        val result = ArrayList<TimedCandle>()
        val chunkSize = 1000

        return query {
            connection.prepareStatement(
                    """
                        SELECT openTime, closeTime, open, close, high, low
                        FROM HistoryCandle
                        WHERE market=? and closeTime<=?
                        ORDER BY closeTime DESC
                    """.trimIndent()
            ).use {
                it.fetchSize = chunkSize
                it.setString(1, market)
                it.setTimestamp(2, time.toSqliteTimestamp())
                it.executeQuery().use { rs ->
                    while (rs.next() && result.size < chunkSize) {
                        val openTime = rs.getTimestamp(1).toInstant()
                        val closeTime = rs.getTimestamp(2).toInstant()
                        val open = rs.getBigDecimal(3)
                        val close = rs.getBigDecimal(4)
                        val high = rs.getBigDecimal(5)
                        val low = rs.getBigDecimal(6)
                        result.add(TimedCandle(
                                openTime..closeTime,
                                Candle(open, close, high, low)
                        ))
                    }
                }
            }
            result
        }
    }

    private fun Instant.toSqliteTimestamp(): Timestamp {
        toEpochMilli() // check if can convert to millis
        return Timestamp.from(this)
    }

    override fun close() = connection.close()

    companion object {
        suspend fun create(path: Path): HistoryCache {
            Class.forName("org.sqlite.JDBC")
            val connection = DriverManager.getConnection("jdbc:sqlite:$path")
            connection.autoCommit = false
            return HistoryCache(connection).apply {
                update()
            }
        }
    }
}