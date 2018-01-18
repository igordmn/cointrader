package exchange.candle

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import util.lang.unsupportedOperation
import java.math.BigDecimal
import java.time.Instant

class ContinuousCandlesSpec : FreeSpec({
    fun instant(intValue: Int) = Instant.ofEpochMilli(intValue.toLong())

    fun candle(open: String, close: String, high: String, low: String): Candle {
        return Candle(
                open = BigDecimal(open),
                close = BigDecimal(close),
                high = BigDecimal(high),
                low = BigDecimal(low)
        )
    }

    fun timedCandle(period: ClosedRange<Instant>, open: String, close: String, high: String, low: String) = TimedCandle(
            period,
            candle(open, close, high, low)
    )

    "fill skipped candles" {
        runBlocking {
            val candles = listOf(
                    timedCandle(period = instant(20)..instant(23), open = "15", close = "17", high = "18", low = "9"),
                    timedCandle(period = instant(19)..instant(20), open = "14", close = "15", high = "17", low = "8"),
                    timedCandle(period = instant(18)..instant(19), open = "13", close = "14", high = "16", low = "7"),
                    timedCandle(period = instant(10)..instant(13), open = "12", close = "11", high = "15", low = "6")
            )

            val candlesWithSkipped = candles
                    .asReceiveChannel()
                    .fillSkipped()
                    .toList()

            with(candlesWithSkipped) {
                size shouldBe 7
                this[0] shouldBe timedCandle(period = instant(23)..Instant.MAX, open = "17", close = "17", high = "17", low = "17")
                this[1] shouldBe timedCandle(period = instant(20)..instant(23), open = "15", close = "17", high = "18", low = "9")
                this[2] shouldBe timedCandle(period = instant(19)..instant(20), open = "14", close = "15", high = "17", low = "8")
                this[3] shouldBe timedCandle(period = instant(18)..instant(19), open = "13", close = "14", high = "16", low = "7")
                this[4] shouldBe timedCandle(period = instant(13)..instant(18), open = "11", close = "11", high = "11", low = "11")
                this[5] shouldBe timedCandle(period = instant(10)..instant(13), open = "12", close = "11", high = "15", low = "6")
                this[6] shouldBe timedCandle(period = Instant.MIN..instant(10), open = "12", close = "12", high = "12", low = "12")
            }
        }
    }

    "cut timedCandle" - {
        val cutCandle00to10 = candle(open = "7567", close = "7457", high = "8568568", low = "45")
        val cutCandle02to10 = candle(open = "7547", close = "3846", high = "8658568", low = "66")
        val cutCandle00to08 = candle(open = "8568", close = "7567", high = "4565747", low = "78")
        val cutCandle02to08 = candle(open = "1568", close = "7567", high = "8658568", low = "44")
        val cutCandleAtBegin = candle(open = "9797", close = "7567", high = "8658568", low = "44")
        val cutCandleAtEnd = candle(open = "9797", close = "7567", high = "8658568", low = "44")

        @Suppress("UNUSED_PARAMETER")
        fun cutInsideCandle(ignored: Candle, t1: Double, t2: Double): Candle = when {
            t1 < 0.0001 && t2 > 0.9999 -> cutCandle00to10
            t1 == 0.2 && t2 > 0.9999 -> cutCandle02to10
            t1 < 0.0001 && t2 == 0.8 -> cutCandle00to08
            t1 == 0.2 && t2 == 0.8 -> cutCandle02to08
            t1 < 0.0001 && t2 < 0.0001 -> cutCandleAtBegin
            t1 > 0.9999 && t2 > 0.9999 -> cutCandleAtEnd
            else -> unsupportedOperation()
        }

        val cutter = TimedCandleCutter(::cutInsideCandle)

        "cut small candle" - {
            val timedCandle = timedCandle(period = instant(20)..instant(30), open = "15", close = "17", high = "18", low = "9")

            "cut whole" {
                cutter.cut(timedCandle, instant(20)..instant(30)) shouldBe TimedCandle(instant(20)..instant(30), cutCandle00to10)
            }

            "cut inside" {
                cutter.cut(timedCandle, instant(22)..instant(30)) shouldBe TimedCandle(instant(22)..instant(30), cutCandle02to10)
                cutter.cut(timedCandle, instant(20)..instant(28)) shouldBe TimedCandle(instant(20)..instant(28), cutCandle00to08)
                cutter.cut(timedCandle, instant(22)..instant(28)) shouldBe TimedCandle(instant(22)..instant(28), cutCandle02to08)
            }

            "cut intersect" {
                cutter.cut(timedCandle, instant(20)..instant(36)) shouldBe TimedCandle(instant(20)..instant(30), cutCandle00to10)
                cutter.cut(timedCandle, instant(22)..instant(36)) shouldBe TimedCandle(instant(22)..instant(30), cutCandle02to10)
                cutter.cut(timedCandle, instant(12)..instant(30)) shouldBe TimedCandle(instant(20)..instant(30), cutCandle00to10)
                cutter.cut(timedCandle, instant(12)..instant(28)) shouldBe TimedCandle(instant(20)..instant(28), cutCandle00to08)
                cutter.cut(timedCandle, instant(18)..instant(30)) shouldBe TimedCandle(instant(20)..instant(30), cutCandle00to10)
                cutter.cut(timedCandle, instant(10)..instant(40)) shouldBe TimedCandle(instant(20)..instant(30), cutCandle00to10)
            }

            "cut outside" {
                cutter.cut(timedCandle, instant(10)..instant(20)) shouldBe null
                cutter.cut(timedCandle, instant(10)..instant(19)) shouldBe null
                cutter.cut(timedCandle, instant(30)..instant(32)) shouldBe null
                cutter.cut(timedCandle, instant(31)..instant(32)) shouldBe null
                cutter.cut(timedCandle, instant(30)..Instant.MAX) shouldBe null
                cutter.cut(timedCandle, Instant.MIN..instant(20)) shouldBe null
            }

            "cut big period" {
                cutter.cut(timedCandle, Instant.MIN..instant(28)) shouldBe TimedCandle(instant(20)..instant(28), cutCandle00to08)
                cutter.cut(timedCandle, instant(22)..Instant.MAX) shouldBe TimedCandle(instant(22)..instant(30), cutCandle02to10)
                cutter.cut(timedCandle, Instant.MIN..Instant.MAX) shouldBe TimedCandle(instant(20)..instant(30), cutCandle00to10)
            }
        }

        "cut big candle with min instant" - {
            val timedCandle = timedCandle(period = Instant.MIN..instant(30), open = "15", close = "17", high = "18", low = "9")

            "cut whole" {
                cutter.cut(timedCandle, Instant.MIN..instant(30)) shouldBe TimedCandle(Instant.MIN..instant(30), cutCandle00to10)
            }

            "cut inside" {
                cutter.cut(timedCandle, Instant.MIN..instant(28)) shouldBe TimedCandle(Instant.MIN..instant(28), cutCandle00to10)
            }

            "cut intersects" {
                cutter.cut(timedCandle, Instant.MIN..instant(32)) shouldBe TimedCandle(Instant.MIN..instant(30), cutCandle00to10)
            }

            "cut outside" {
                cutter.cut(timedCandle, instant(30)..instant(34)) shouldBe null
                cutter.cut(timedCandle, instant(31)..instant(34)) shouldBe null
                cutter.cut(timedCandle, instant(30)..Instant.MAX) shouldBe null
            }

            "cut small period" {
                cutter.cut(timedCandle, instant(20)..instant(28)) shouldBe TimedCandle(instant(20)..instant(28), cutCandleAtEnd)
                cutter.cut(timedCandle, instant(20)..instant(30)) shouldBe TimedCandle(instant(20)..instant(30), cutCandleAtEnd)
                cutter.cut(timedCandle, instant(20)..instant(36)) shouldBe TimedCandle(instant(20)..instant(30), cutCandleAtEnd)
            }

            "cut big period" {
                cutter.cut(timedCandle, Instant.MIN..Instant.MAX) shouldBe TimedCandle(Instant.MIN..instant(30), cutCandle00to10)
            }
        }

        "cut big candle with max instant" - {
            val timedCandle = timedCandle(period = instant(20)..Instant.MAX, open = "15", close = "17", high = "18", low = "9")

            "cut whole" {
                cutter.cut(timedCandle, instant(20)..Instant.MAX) shouldBe TimedCandle(instant(20)..Instant.MAX, cutCandle00to10)
            }

            "cut inside" {
                cutter.cut(timedCandle, instant(22)..Instant.MAX) shouldBe TimedCandle(instant(22)..Instant.MAX, cutCandle00to10)
            }

            "cut intersects" {
                cutter.cut(timedCandle, instant(18)..Instant.MAX) shouldBe TimedCandle(instant(20)..Instant.MAX, cutCandle00to10)
            }

            "cut outside" {
                cutter.cut(timedCandle, instant(16)..instant(20)) shouldBe null
                cutter.cut(timedCandle, instant(16)..instant(19)) shouldBe null
                cutter.cut(timedCandle, Instant.MIN..instant(20)) shouldBe null
            }

            "cut small period" {
                cutter.cut(timedCandle, instant(22)..instant(30)) shouldBe TimedCandle(instant(22)..instant(30), cutCandleAtBegin)
                cutter.cut(timedCandle, instant(20)..instant(30)) shouldBe TimedCandle(instant(20)..instant(30), cutCandleAtBegin)
                cutter.cut(timedCandle, instant(14)..instant(24)) shouldBe TimedCandle(instant(20)..instant(24), cutCandleAtBegin)
            }

            "cut big period" {
                cutter.cut(timedCandle, Instant.MIN..Instant.MAX) shouldBe TimedCandle(instant(20)..Instant.MAX, cutCandle00to10)
            }
        }
    }
})