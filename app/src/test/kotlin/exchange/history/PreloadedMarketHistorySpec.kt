package exchange.history

import data.HistoryCache
import exchange.binance.api.binanceAPI
import exchange.candle.Candle
import exchange.candle.TimedCandle
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking
import util.lang.InstantRange
import util.lang.truncatedTo
import java.math.BigDecimal
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

class PreloadedMarketHistorySpec : FreeSpec({
    fun instant(millis: Int) = Instant.ofEpochMilli(millis.toLong())
    fun instantRange(millis: IntRange) = instant(millis.start)..instant(millis.endInclusive)

    fun candle(timeRange: InstantRange): TimedCandle {
        return TimedCandle(
                timeRange.start..timeRange.endInclusive,
                Candle(
                        open = BigDecimal("5"),
                        close = BigDecimal("6"),
                        high = BigDecimal("77"),
                        low = BigDecimal("1")
                ))
    }

    fun candle(millisRange: IntRange) = candle(instantRange(millisRange))

    val startDate = instant(10000)
    val period = Duration.ofMillis(60)

    val original = object : MarketHistory {
        override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
            val lastClose = time.truncatedTo(period) - Duration.ofMillis(1)
            var close = lastClose
            while (close - period >= startDate) {
                val open = close - period + Duration.ofMillis(1)
                send(candle(open..close))
                close -= period
            }
        }
    }

    "test original history" - {
        fun rangesBefore(millis: Int, count: Int) = runBlocking {
            original.candlesBefore(instant(millis)).map {
                it.timeRange
            }.take(count).toList()
        }

        "test1" {
            rangesBefore(20000, count = 3) shouldBe listOf(
                    instantRange(19920..19979),
                    instantRange(19860..19919),
                    instantRange(19800..19859)
            )
        }

        "test2" {
            rangesBefore(19980, count = 3) shouldBe listOf(
                    instantRange(19920..19979),
                    instantRange(19860..19919),
                    instantRange(19800..19859)
            )
        }

        "test3" {
            rangesBefore(19981, count = 3) shouldBe listOf(
                    instantRange(19920..19979),
                    instantRange(19860..19919),
                    instantRange(19800..19859)
            )
        }

        "test5" {
            rangesBefore(19979, count = 3) shouldBe listOf(
                    instantRange(19860..19919),
                    instantRange(19800..19859),
                    instantRange(19740..19799)
            )
        }

        "test6" {
            rangesBefore(20040, count = 3) shouldBe listOf(
                    instantRange(19980..20039),
                    instantRange(19920..19979),
                    instantRange(19860..19919)
            )
        }

        "test7" {
            rangesBefore(10080, count = 3) shouldBe listOf(
                    instantRange(10020..10079)
            )
        }
    }

    fun usePreloadedHistory(action: suspend (PreloadedMarketHistory) -> Unit) = runBlocking {
        val tempFile = Files.createTempFile("test", ".tmp")
        try {
            HistoryCache.create(tempFile).use {
                val preloadedHistory = PreloadedMarketHistory(it, "LTC", original, Duration.ofMinutes(1))
                action(preloadedHistory)
            }
        } finally {
            Files.delete(tempFile)
        }
    }

    "get candles without preload" {
        usePreloadedHistory {
            it.candlesBefore(Instant.MAX).toList() shouldBe emptyList<TimedCandle>()
            it.candlesBefore(Instant.MIN).toList() shouldBe emptyList<TimedCandle>()
        }
    }

    "preload at start" - {
        "no candle 1" {
            usePreloadedHistory {
                it.preload(Instant.MIN..instant(10079))
                it.candlesBefore(Instant.MAX).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(10080)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(Instant.MIN).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "no candle 2" {
            usePreloadedHistory {
                it.preload(instantRange(10021..10080))
                it.candlesBefore(Instant.MAX).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(10080)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(Instant.MIN).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "no candle 3" {
            usePreloadedHistory {
                it.preload(instantRange(10020..10079))
                it.candlesBefore(Instant.MAX).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(10080)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(Instant.MIN).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "one candle 1" {
            usePreloadedHistory {
                it.preload(instantRange(10020..10080))
                it.candlesBefore(Instant.MAX).toList() shouldBe listOf(candle(10020..10079))
//                it.candlesBefore(instant(10080)).toList() shouldBe listOf(candle(10020..10079))
//                it.candlesBefore(instant(10070)).toList() shouldBe emptyList<TimedCandle>()
//                it.candlesBefore(Instant.MIN).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "one candle 2" {
            usePreloadedHistory {
                it.preload(Instant.MIN..instant(10080))
                it.candlesBefore(Instant.MAX).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10080)).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10070)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(Instant.MIN).toList() shouldBe emptyList<TimedCandle>()
            }
        }
    }
})