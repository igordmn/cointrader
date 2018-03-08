package old.exchange.binance

import old.exchange.binance.market.BinanceMarketHistory
import old.exchange.candle.LinearApproximatedPricesFactory
import old.exchange.candle.approximateCandleNormalizer
import old.exchange.history.NormalizedMarketHistory
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.channels.take
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.helpers.NOPLogger
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class BinanceHistorySpec : StringSpec({
    val operationScale = 4
    val marketName = BinanceConstants().marketName("USDT", "BTC")!!
    val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
    val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
    val period = Duration.ofMinutes(5)
    val history = NormalizedMarketHistory(BinanceMarketHistory(marketName, binanceAPI, NOPLogger.NOP_LOGGER), normalizer, period)

    "get candles" {
        runBlocking {
            val historyEndTime =
                    ZonedDateTime.of(2018, 1, 10, 12, 0, 2, 0, ZoneOffset.UTC)
                    .toInstant()
                    .truncatedTo(ChronoUnit.MINUTES)
            val count = 10

            val candles = history.candlesBefore(historyEndTime).take(count).toList()
            val candlesStr = candles.joinToString("\n")
            println("Candles before $historyEndTime with period $period:\n$candlesStr")

            candles.size shouldBe count
        }
    }
})