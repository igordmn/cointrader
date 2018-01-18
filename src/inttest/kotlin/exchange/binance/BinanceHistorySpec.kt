package exchange.binance

import exchange.binance.api.binanceAPI
import exchange.binance.market.BinanceMarketHistory
import exchange.candle.LinearApproximatedPricesFactory
import exchange.candle.approximateCandleNormalizer
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class BinanceHistorySpec : StringSpec({
    val operationScale = 4
    val api = binanceAPI()
    val marketName = BinanceInfo().marketName("USDT", "BTC")!!
    val approximatedPricesFactory = LinearApproximatedPricesFactory(operationScale)
    val normalizer = approximateCandleNormalizer(approximatedPricesFactory)
    val history = BinanceMarketHistory(marketName, api, normalizer)

    "get candles" {
        runBlocking {
            val historyEndTime =
                    ZonedDateTime.of(2018, 1, 10, 12, 0, 2, 0, ZoneOffset.UTC)
                    .toInstant()
                    .truncatedTo(ChronoUnit.MINUTES)
            val count = 10
            val period = Duration.ofMinutes(5)

            val candles = history.candlesBefore(historyEndTime, count, period)
            val candlesStr = candles.joinToString("\n")
            println("Candles before $historyEndTime with period $period:\n$candlesStr")

            candles.size shouldBe count
        }
    }
})