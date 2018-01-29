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
import util.lang.min
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
    val endDate = instant(21000)
    val period = Duration.ofMillis(60)

    val original = object : MarketHistory {
        override fun candlesBefore(time: Instant): ReceiveChannel<TimedCandle> = produce {
            val lastClose = min(time, endDate).truncatedTo(period) - Duration.ofMillis(1)
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

        "test8" {
            rangesBefore(22000, count = 2) shouldBe listOf(
                    instantRange(20940..20999),
                    instantRange(20880..20939)
            )
        }

        "test9" {
            rangesBefore(21000, count = 2) shouldBe listOf(
                    instantRange(20940..20999),
                    instantRange(20880..20939)
            )
        }

        "test10" {
            rangesBefore(20999, count = 2) shouldBe listOf(
                    instantRange(20880..20939),
                    instantRange(20820..20879)
            )
        }
    }

    fun usePreloadedHistory(action: suspend (PreloadedMarketHistory) -> Unit) = runBlocking {
        val tempFile = Files.createTempFile("test", ".tmp")
        try {
            HistoryCache.create(tempFile).use {
                val preloadedHistory = PreloadedMarketHistory(it, "LTC", original, period)
                action(preloadedHistory)
            }
        } finally {
            Files.delete(tempFile)
        }
    }

    "get candles without preload" {
        usePreloadedHistory {
            it.candlesBefore(instant(9999999)).toList() shouldBe emptyList<TimedCandle>()
            it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
        }
    }

    "preload at start" - {
        "no candles 1" {
            usePreloadedHistory {
                it.preload(instantRange(100..10079))
                it.candlesBefore(instant(9999999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(10080)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "no candles 2" {
            usePreloadedHistory {
                it.preload(instantRange(10021..10080))
                it.candlesBefore(instant(9999999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(10080)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "no candles 3" {
            usePreloadedHistory {
                it.preload(instantRange(10020..10079))
                it.candlesBefore(instant(9999999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(10080)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "one candle 1" {
            usePreloadedHistory {
                it.preload(instantRange(10020..10080))
                it.candlesBefore(instant(9999999)).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10080)).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10079)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "one candle 2" {
            usePreloadedHistory {
                it.preload(instantRange(99..10080))
                it.candlesBefore(instant(9999999)).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10080)).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10079)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "two candles" {
            usePreloadedHistory {
                it.preload(instantRange(99..10160))
                it.candlesBefore(instant(9999999)).toList() shouldBe listOf(candle(10080..10139), candle(10020..10079))
                it.candlesBefore(instant(10140)).toList() shouldBe listOf(candle(10080..10139), candle(10020..10079))
                it.candlesBefore(instant(10139)).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10080)).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10079)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }
    }

    "preload at end" - {
        "no candles 1" {
            usePreloadedHistory {
                it.preload(instantRange(21000..9999999))
                it.candlesBefore(instant(9999999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(21000)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "no candles 2" {
            usePreloadedHistory {
                it.preload(instantRange(20999..9999999))
                it.candlesBefore(instant(9999999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(21000)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(20999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "no candles 3" {
            usePreloadedHistory {
                it.preload(instantRange(20941..21000))
                it.candlesBefore(instant(9999999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(21000)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "one candle 1" {
            usePreloadedHistory {
                it.preload(instantRange(20940..21000))
                it.candlesBefore(instant(9999999)).toList() shouldBe listOf(candle(20940..20999))
                it.candlesBefore(instant(21000)).toList() shouldBe listOf(candle(20940..20999))
                it.candlesBefore(instant(20999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "one candle 2" {
            usePreloadedHistory {
                it.preload(instantRange(20881..21000))
                it.candlesBefore(instant(9999999)).toList() shouldBe listOf(candle(20940..20999))
                it.candlesBefore(instant(21000)).toList() shouldBe listOf(candle(20940..20999))
                it.candlesBefore(instant(20999)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "two candles" {
            usePreloadedHistory {
                it.preload(instantRange(20880..21000))
                it.candlesBefore(instant(9999999)).toList() shouldBe listOf(candle(20940..20999), candle(20880..20939))
                it.candlesBefore(instant(21000)).toList() shouldBe listOf(candle(20940..20999), candle(20880..20939))
                it.candlesBefore(instant(20999)).toList() shouldBe listOf(candle(20880..20939))
                it.candlesBefore(instant(20940)).toList() shouldBe listOf(candle(20880..20939))
                it.candlesBefore(instant(20939)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "one candle 3" {
            usePreloadedHistory {
                it.preload(instantRange(20880..20999))
                it.candlesBefore(instant(9999999)).toList() shouldBe listOf(candle(20880..20939))
                it.candlesBefore(instant(21000)).toList() shouldBe listOf(candle(20880..20939))
                it.candlesBefore(instant(20999)).toList() shouldBe listOf(candle(20880..20939))
                it.candlesBefore(instant(20940)).toList() shouldBe listOf(candle(20880..20939))
                it.candlesBefore(instant(20939)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }
    }

    "preload in middle" - {
        "two candles" {
            usePreloadedHistory {
                it.preload(instantRange(17880..18000))
                it.candlesBefore(instant(9999999)).toList() shouldBe listOf(candle(17940..17999), candle(17880..17939))
                it.candlesBefore(instant(18000)).toList() shouldBe listOf(candle(17940..17999), candle(17880..17939))
                it.candlesBefore(instant(17999)).toList() shouldBe listOf(candle(17880..17939))
                it.candlesBefore(instant(17940)).toList() shouldBe listOf(candle(17880..17939))
                it.candlesBefore(instant(17939)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }

        "all candles" {
            usePreloadedHistory {
                it.preload(instantRange(99..9999999))
                it.candlesBefore(instant(9999999)).take(2).toList() shouldBe listOf(candle(20940..20999), candle(20880..20939))
                it.candlesBefore(instant(21000)).take(2).toList() shouldBe listOf(candle(20940..20999), candle(20880..20939))
                it.candlesBefore(instant(20999)).take(2).toList() shouldBe listOf(candle(20880..20939), candle(20820..20879))
                it.candlesBefore(instant(10080)).toList() shouldBe listOf(candle(10020..10079))
                it.candlesBefore(instant(10079)).toList() shouldBe emptyList<TimedCandle>()
                it.candlesBefore(instant(99)).toList() shouldBe emptyList<TimedCandle>()
            }
        }
    }


})