package data

import exchange.candle.Candle
import exchange.candle.TimedCandle
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import util.concurrent.windowedWithPartial
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.sql.Timestamp
import kotlin.system.measureTimeMillis


class HistoryCache private constructor(private val connection: Connection) : AutoCloseable {
    private fun update() {
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

    fun startTimeOf(market: String): Instant = metaStartTimeOf(market) ?: minTimeOf(market) ?: Instant.MAX
    fun endTimeOf(market: String): Instant = metaEndTimeOf(market) ?: maxTimeOf(market) ?: Instant.MIN

    private fun metaStartTimeOf(market: String): Instant? = selectSingleInstant(market,
            "SELECT startTime FROM HistoryCandleMeta WHERE market=?"
    )

    private fun metaEndTimeOf(market: String): Instant? = selectSingleInstant(market,
            "SELECT endTime FROM HistoryCandleMeta WHERE market=?"
    )

    private fun minTimeOf(market: String): Instant? = selectSingleInstant(market,
            "SELECT min(openTime) FROM HistoryCandle WHERE market=?"
    )

    private fun maxTimeOf(market: String): Instant? = selectSingleInstant(market,
            "SELECT max(closeTime) FROM HistoryCandle WHERE market=?"
    )

    private fun selectSingleInstant(market: String, sql: String): Instant? = connection.prepareStatement(sql).use {
        it.setString(1, market)
        it.executeQuery().use { rs ->
            val hasRows = rs.next()
            return if (hasRows) {
                val timestamp = rs.getTimestamp(1)
                timestamp?.toInstant()
            } else {
                null
            }
        }
    }

    suspend fun insertCandles(
            market: String,
            candles: ReceiveChannel<TimedCandle>,
            startTime: Instant,
            endTime: Instant
    ) {
        candles.windowedWithPartial(size = 5000).consumeEach { candlesBatch ->
            connection.prepareStatement("INSERT INTO HistoryCandle VALUES (?,?,?,?,?,?,?)").use {
                println("GGGG " + measureTimeMillis {
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
                })
            }
        }
        setFilled(market, startTime, endTime)
    }

    private fun setFilled(market: String, startTime: Instant, endTime: Instant) {
        connection.prepareStatement("INSERT OR REPLACE INTO HistoryCandleMeta VALUES (?,?,?)").use {
            it.setString(1, market)
            it.setTimestamp(2, Timestamp.from(startTime))
            it.setTimestamp(3, Timestamp.from(endTime))
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

    private fun candlesChunkBefore(market: String, time: Instant): List<TimedCandle> {
        val result = ArrayList<TimedCandle>()
        val chunkSize = 1000
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
        return result
    }

    override fun close() = Unit

    companion object {
        fun create(path: Path): HistoryCache {
            Class.forName("org.sqlite.JDBC")
            val connection = DriverManager.getConnection("jdbc:sqlite:$path")
            return HistoryCache(connection).apply {
                update()
            }
        }
    }
}