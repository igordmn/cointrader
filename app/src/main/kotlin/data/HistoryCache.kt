package data

import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.newSingleThreadContext
import util.concurrent.windowedWithPartial
import util.lang.InstantRange
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.sql.Timestamp

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
                CREATE TABLE IF NOT EXISTS HistoryCandleMeta(
                    market VARCHAR(20) NOT NULL,
                    startTime TIMESTAMP NOT NULL,
                    endTime TIMESTAMP NOT NULL,
                    PRIMARY KEY (market)
                );
                CREATE UNIQUE INDEX IF NOT EXISTS HistoryCandle_market_closeTime ON HistoryCandle(market, closeTime);
                CREATE UNIQUE INDEX IF NOT EXISTS HistoryCandle_market_openTime ON HistoryCandle(market, openTime);
            """.trimIndent()
            )
        }
    }

    suspend fun filledRange(market: String): InstantRange? {
        val meta = metaFilledRange(market)
        return if (meta != null) {
            meta
        } else {
            val min = minTimeOf(market)
            val max = maxTimeOf(market)
            if (min != null && max != null) {
                min..max
            } else {
                null
            }
        }
    }

    private fun metaFilledRange(market: String): InstantRange? {
        return connection.prepareStatement("SELECT startTime, endTime FROM HistoryCandleMeta WHERE market=?").use {
            it.setString(1, market)
            it.executeQuery().use { rs ->
                val hasRows = rs.next()
                if (hasRows) {
                    val startTime = rs.getTimestamp(1)
                    val endTime = rs.getTimestamp(2)
                    startTime.toInstant()..endTime.toInstant()
                } else {
                    null
                }
            }
        }
    }

    private suspend fun minTimeOf(market: String): Instant? = selectSingleInstant(market,
            "SELECT min(openTime) FROM HistoryCandle WHERE market=?"
    )

    private suspend fun maxTimeOf(market: String): Instant? = selectSingleInstant(market,
            "SELECT max(closeTime) FROM HistoryCandle WHERE market=?"
    )

    private suspend fun selectSingleInstant(market: String, sql: String): Instant? {
        return query {
            connection.prepareStatement(sql).use {
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
    }

    suspend fun insertCandles(
            market: String,
            candles: ReceiveChannel<TimedCandle>,
            allFilledRange: InstantRange
    ) {
        candles.windowedWithPartial(size = 1000).consumeEach { candlesBatch ->
            modify {
                connection.prepareStatement("INSERT INTO HistoryCandle VALUES (?,?,?,?,?,?,?)").use {
                    for (candle in candlesBatch) {
                        it.setString(1, market)
                        it.setTimestamp(2, Timestamp.from(candle.timeRange.start))
                        it.setTimestamp(3, Timestamp.from(candle.timeRange.endInclusive))
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

        setFilled(market, allFilledRange)
    }

    private suspend fun setFilled(market: String, timeRange: InstantRange) = modify {
        connection.prepareStatement("INSERT OR REPLACE INTO HistoryCandleMeta VALUES (?,?,?)").use {
            it.setString(1, market)
            it.setTimestamp(2, Timestamp.from(timeRange.start))
            it.setTimestamp(3, Timestamp.from(timeRange.endInclusive))
            it.executeUpdate()
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
                it.setTimestamp(2, Timestamp.from(time))
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