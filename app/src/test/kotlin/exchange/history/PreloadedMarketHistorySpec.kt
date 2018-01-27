package exchange.history

import exchange.candle.Candle
import exchange.candle.TimedCandle
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import org.mapdb.DBMaker
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class PreloadedMarketHistorySpec : FreeSpec({
    fun instant(millis: Int) = Instant.ofEpochMilli(millis.toLong())

    fun candle(open: String, close: String, high: String, low: String): Candle {
        return Candle(
                open = BigDecimal(open),
                close = BigDecimal(close),
                high = BigDecimal(high),
                low = BigDecimal(low)
        )
    }

    fun timedCandle(period: IntRange) = TimedCandle(
            instant(period.start)..instant(period.endInclusive),
            candle("56", "77", "888", "7")
    )

    val candle0 = timedCandle(70..79)
    val candle1 = timedCandle(60..69)
    val candle2 = timedCandle(50..60)
    val candle3 = timedCandle(40..45)

    class TestHistory : MarketHistory {
        var candles = listOf(candle1, candle2, candle3)

        override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> {
            return candles.filter { it.timeRange.endInclusive <= time }.asReceiveChannel()
        }
    }

    val testHistory = TestHistory()
    DBMaker.memoryDB().transactionEnable().make().use {
        val history = PreloadedMarketHistory(it, "table", testHistory, Duration.ofMillis(10))

        "get candles by one call" - {
            "get candles before big time" {
                runBlocking {
                    history.preloadBefore(instant(100))
                    history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
                }
            }

            "get candles before end plus period" {
                runBlocking {
                    history.preloadBefore(instant(79))
                    history.candlesBefore(instant(79)).toList() shouldBe listOf(candle1, candle2, candle3)
                }
            }

            "get candles before end" {
                runBlocking {
                    history.preloadBefore(instant(69))
                    history.candlesBefore(instant(69)).toList() shouldBe listOf(candle1, candle2, candle3)
                }
            }

            "get candles before end minus 1" {
                runBlocking {
                    history.preloadBefore(instant(68))
                    history.candlesBefore(instant(68)).toList() shouldBe listOf(candle2, candle3)
                }
            }

            "get candles before first end" {
                runBlocking {
                    history.preloadBefore(instant(60))
                    history.candlesBefore(instant(60)).toList() shouldBe listOf(candle2, candle3)
                }
            }

            "get candles before first end minus 1" {
                runBlocking {
                    history.preloadBefore(instant(59))
                    history.candlesBefore(instant(59)).toList() shouldBe listOf(candle3)
                }
            }

            "get candles before last end minus 1" {
                runBlocking {
                    history.preloadBefore(instant(44))
                    history.candlesBefore(instant(44)).toList() shouldBe emptyList<TimedCandle>()
                }
            }
        }

        "get candles by multiple calls" - {
            "get empty candles then all candles" {
                runBlocking {
                    history.preloadBefore(instant(44))
                    history.candlesBefore(instant(44)).toList() shouldBe emptyList<TimedCandle>()
                    history.preloadBefore(instant(100))
                    history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
                }
            }

            "get first candle then all candles" {
                runBlocking {
                    history.preloadBefore(instant(45))
                    history.candlesBefore(instant(45)).toList() shouldBe listOf(candle3)
                    history.preloadBefore(instant(100))
                    history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
                }
            }

            "get first candle then first and second candles" {
                runBlocking {
                    history.preloadBefore(instant(45))
                    history.candlesBefore(instant(45)).toList() shouldBe listOf(candle3)
                    history.preloadBefore(instant(65))
                    history.candlesBefore(instant(65)).toList() shouldBe listOf(candle2, candle3)
                }
            }

            "get all candles then first candle" {
                runBlocking {
                    history.preloadBefore(instant(100))
                    history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
                    history.preloadBefore(instant(45))
                    history.candlesBefore(instant(45)).toList() shouldBe listOf(candle3)
                }
            }
        }

        "get candles with adding new" - {
            "get all candles, add new candle, get before big time" {
                runBlocking {
                    history.preloadBefore(instant(100))
                    history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
                    testHistory.candles = listOf(candle0) + testHistory.candles
                    history.preloadBefore(instant(100))
                    history.candlesBefore(instant(100)).toList() shouldBe listOf(candle0, candle1, candle2, candle3)
                }
            }

            "get all candles, add new candle, get before small time" {
                runBlocking {
                    history.preloadBefore(instant(79))
                    history.candlesBefore(instant(79)).toList() shouldBe listOf(candle1, candle2, candle3)
                    testHistory.candles = listOf(candle0) + testHistory.candles
                    history.preloadBefore(instant(79))
                    history.candlesBefore(instant(79)).toList() shouldBe listOf(candle0, candle1, candle2, candle3)
                }
            }

            "get all candles, add new candle, get before very small time" {
                runBlocking {
                    history.preloadBefore(instant(78))
                    history.candlesBefore(instant(78)).toList() shouldBe listOf(candle1, candle2, candle3)
                    testHistory.candles = listOf(candle0) + testHistory.candles
                    history.preloadBefore(instant(78))
                    history.candlesBefore(instant(78)).toList() shouldBe listOf(candle1, candle2, candle3)
                }
            }
        }
    }
})