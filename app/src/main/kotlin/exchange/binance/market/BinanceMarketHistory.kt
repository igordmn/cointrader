package exchange.binance.market

import com.binance.api.client.constant.BinanceApiConstants
import com.binance.api.client.domain.market.Candlestick
import exchange.binance.BinanceConstants
import exchange.history.MarketHistory
import exchange.binance.api.BinanceAPI
import exchange.candle.Candle
import exchange.candle.CandleNormalizer
import exchange.candle.TimedCandle
import exchange.history.PreloadedMarketHistory
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.*
import org.mapdb.DB
import org.mapdb.DBMaker
import org.slf4j.Logger
import util.lang.instantRangeOfMilli
import util.log.logger
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

class BinanceMarketHistory(
        private val name: String,
        private val api: BinanceAPI,
        private val log: Logger
) : MarketHistory {
    override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
        var timeIt = time

        while(true) {
            val chunk = candlesChunkByMinuteBefore(timeIt)
            if (chunk.isEmpty()) {
                break
            }
            chunk.forEach {
                send(it)
            }
            timeIt = chunk.last().timeRange.start
        }

        close()
    }

    private suspend fun candlesChunkByMinuteBefore(time: Instant): List<TimedCandle> {
        log.info("Load candles for $name before $time")
        fun Candlestick.toLocalCandle() = TimedCandle(
                instantRangeOfMilli(openTime, closeTime),
                Candle(
                        BigDecimal(open),
                        BigDecimal(close),
                        BigDecimal(high),
                        BigDecimal(low)
                )
        )
        fun List<TimedCandle>.dropIncompleteCandle(end: Instant): List<TimedCandle> {
            return if (isNotEmpty() && first().timeRange.endInclusive != end) {
                drop(1)
            } else {
                this
            }
        }

        val maxBinanceCount = 500
        val end = time.truncatedTo(ChronoUnit.MINUTES) - Duration.ofMillis(1)
        val result = api
                .getCandlestickBars(name, "1m", maxBinanceCount, null, end.toEpochMilli())
                .asSequence()
                .map(Candlestick::toLocalCandle)
                .filter { it.timeRange.endInclusive <= end }
                .filter { it.timeRange.endInclusive > it.timeRange.start }
                .toList()
                .asReversed()
                .dropIncompleteCandle(end)

        result.zipWithNext().forEach { (current, next) ->
            require(next.timeRange.endInclusive <= current.timeRange.start)
        }

        return result
    }
}

fun makeBinanceCacheDB(): DB {
    val folder: Path = Paths.get("data/cache/binance/")
    Files.createDirectories(folder)
    val file = folder.resolve("history").toFile()
    return DBMaker.fileDB(file).transactionEnable().make()
}

fun preloadedBinanceMarketHistory(db: DB, api: BinanceAPI, name: String) = PreloadedMarketHistory(
        db,
        name,
        BinanceMarketHistory(name, api, logger(BinanceMarketHistory::class)),
        Duration.ofMinutes(1)
)

class PreloadedBinanceMarketHistories(
        private val constants: BinanceConstants,
        private val api: BinanceAPI,
        private val mainCoin: String,
        private val altCoins: List<String>
): AutoCloseable {
    private val db = makeBinanceCacheDB()
    private val map = ConcurrentHashMap<String, PreloadedMarketHistory>()

    operator fun get(name: String): PreloadedMarketHistory = map[name]!!

    suspend fun preloadBefore(time: Instant) {
        altCoins
                .mapNotNull { constants.marketName(mainCoin, it) }
                .map { name ->
                    async {
                        preloadBefore(name, time)
                    }
                }.forEach {
                    it.await()
                }
    }

    private suspend fun preloadBefore(name: String, time: Instant) {
        val history = map.getOrPut(name) { preloadedBinanceMarketHistory(db, api, name) }
        history.preloadBefore(time)
    }

    override fun close() = db.close()
}