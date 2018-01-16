package exchange.binance

import com.binance.api.client.BinanceApiClientFactory
import exchange.binance.market.BinanceMarketHistory
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BinanceHistorySpec : StringSpec({
    val factory = BinanceApiClientFactory.newInstance()
    val client = factory.newAsyncRestClient()
    val marketName = BinanceInfo().marketName("USDT", "BTC")!!
    val history = BinanceMarketHistory(marketName, client)

    "get candles" {
        runBlocking {
            val historyEndTime = ZonedDateTime.of(2018, 1, 10, 12, 0, 2, 0, ZoneOffset.UTC).toInstant()
            val count = 10
            val period = Duration.ofMinutes(5)

            val candles = history.candlesBefore(historyEndTime, count, period)
            val candlesStr = candles.joinToString("\n")
            println("Candles before $historyEndTime:\n$candlesStr with period $period")

            candles.size shouldBe count
        }
    }
})