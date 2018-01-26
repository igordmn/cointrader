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

class CachedMarketHistorySpec : FreeSpec({
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

    val candle1 = timedCandle(60..69)
    val candle2 = timedCandle(50..60)
    val candle3 = timedCandle(40..45)
    val candle4 = timedCandle(70..79)

    class TestHistory : MarketHistory {
        val candles = arrayListOf(candle1, candle2, candle3)

        override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> {
            return candles.asReceiveChannel()
        }
    }

    val testHistory = TestHistory()
    val history = CachedMarketHistory(DBMaker.memoryDB(), testHistory, Duration.ofMillis(10))

    "get candles by one call" - {
        "get candles before big time" {
            runBlocking {
                history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
            }
        }

        "get candles before end plus period" {
            runBlocking {
                history.candlesBefore(instant(79)).toList() shouldBe listOf(candle1, candle2, candle3)
            }
        }

        "get candles before end" {
            runBlocking {
                history.candlesBefore(instant(69)).toList() shouldBe listOf(candle1, candle2, candle3)
            }
        }

        "get candles before end minus 1" {
            runBlocking {
                history.candlesBefore(instant(68)).toList() shouldBe listOf(candle1, candle2)
            }
        }

        "get candles before first end" {
            runBlocking {
                history.candlesBefore(instant(60)).toList() shouldBe listOf(candle1, candle2)
            }
        }

        "get candles before first end minus 1" {
            runBlocking {
                history.candlesBefore(instant(59)).toList() shouldBe listOf(candle1)
            }
        }

        "get candles before last end minus 1" {
            runBlocking {
                history.candlesBefore(instant(44)).toList() shouldBe emptyList<TimedCandle>()
            }
        }
    }

    "get candles by multiple calls" - {
        "get empty candles then all candles" {
            runBlocking {
                history.candlesBefore(instant(44)).toList() shouldBe emptyList<TimedCandle>()
                history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
            }
        }

        "get first candle then all candles" {
            runBlocking {
                history.candlesBefore(instant(45)).toList() shouldBe listOf(candle1)
                history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
            }
        }

        "get first candle then first and second candles" {
            runBlocking {
                history.candlesBefore(instant(45)).toList() shouldBe listOf(candle1)
                history.candlesBefore(instant(65)).toList() shouldBe listOf(candle1, candle2)
            }
        }

        "get all candles then first candle" {
            runBlocking {
                history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
                history.candlesBefore(instant(45)).toList() shouldBe listOf(candle1)
            }
        }
    }

    "get candles with adding new" - {
        "get all candles, add new candle, get before big time" {
            runBlocking {
                history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3)
                testHistory.candles.add(candle4)
                history.candlesBefore(instant(100)).toList() shouldBe listOf(candle1, candle2, candle3, candle4)
            }
        }

        "get all candles, add new candle, get before small time" {
            runBlocking {
                history.candlesBefore(instant(79)).toList() shouldBe listOf(candle1, candle2, candle3)
                testHistory.candles.add(candle4)
                history.candlesBefore(instant(79)).toList() shouldBe listOf(candle1, candle2, candle3, candle4)
            }
        }

        "get all candles, add new candle, get before very small time" {
            runBlocking {
                history.candlesBefore(instant(78)).toList() shouldBe listOf(candle1, candle2, candle3)
                testHistory.candles.add(candle4)
                history.candlesBefore(instant(78)).toList() shouldBe listOf(candle1, candle2, candle3)
            }
        }
    }
})