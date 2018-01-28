package exchange.binance.market

import com.binance.api.client.domain.market.Candlestick
import data.HistoryCache
import exchange.binance.BinanceConstants
import exchange.binance.api.BinanceAPI
import exchange.candle.Candle
import exchange.candle.TimedCandle
import exchange.history.MarketHistory
import exchange.history.PreloadedMarketHistory
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import org.slf4j.Logger
import util.lang.instantRangeOfMilli
import util.log.logger
import java.math.BigDecimal
import java.nio.file.Files
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

        while (true) {
            val chunk = candlesChunkByMinuteBefore(timeIt)
            if (chunk.isEmpty()) {
                break
            }
            chunk.forEach {
                send(it)
            }
            timeIt = chunk.last().timeRange.start
        }
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
                .filter { Instant.ofEpochMilli(it.closeTime) <= end }
                .filter { it.closeTime > it.openTime }
                .map(Candlestick::toLocalCandle)
                .toList()
                .asReversed()
                .dropIncompleteCandle(end)

        result.zipWithNext().forEach { (current, next) ->
            require(next.timeRange.endInclusive <= current.timeRange.start)
        }

        return result
    }
}

suspend fun makeBinanceCacheDB(): HistoryCache {
    val path = Paths.get("data/cache/binance")
    Files.createDirectories(path.parent)
    return HistoryCache.create(path)
}

fun preloadedBinanceMarketHistory(cache: HistoryCache, api: BinanceAPI, name: String) = PreloadedMarketHistory(
        cache,
        name,
        BinanceMarketHistory(name, api, logger(BinanceMarketHistory::class)),
        Duration.ofMinutes(1)
)

class PreloadedBinanceMarketHistories(
        private val cache: HistoryCache,
        private val constants: BinanceConstants,
        private val api: BinanceAPI,
        private val mainCoin: String,
        private val altCoins: List<String>
) {
    private val map = ConcurrentHashMap<String, PreloadedMarketHistory>()

    operator fun get(name: String): PreloadedMarketHistory {
        return map[name]!!
    }

    suspend fun preloadBefore(time: Instant) {
        altCoins
                .mapNotNull { constants.marketName(mainCoin, it) ?: constants.marketName(it, mainCoin) }
                .map { name ->
                    async {
                        preloadBefore(name, time)
                    }
                }.forEach {
                    it.await()
                }
    }

    private suspend fun preloadBefore(name: String, time: Instant) {
        val history = map.getOrPut(name) { preloadedBinanceMarketHistory(cache, api, name) }
        history.preloadBefore(time)
    }
}