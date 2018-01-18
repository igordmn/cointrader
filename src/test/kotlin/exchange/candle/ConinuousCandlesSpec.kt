package exchange.candle

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import util.lang.unsupportedOperation
import java.math.BigDecimal
import java.time.Instant

class ConinuousCandlesSpec : FreeSpec({
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

    "cut timedCandle" {
        val timedCandle = timedCandle(period = instant(20)..instant(30), open = "15", close = "17", high = "18", low = "9")
        val cutCandle00to10 = candle(open = "756", close = "7457", high = "345", low = "7457")
        val cutCandle02to10 = candle(open = "7547", close = "346", high = "754", low = "346346")
        val cutCandle00to08 = candle(open = "8568", close = "75657", high = "346", low = "7547")
        val cutCandle02to08 = candle(open = "8568", close = "75657", high = "346", low = "7547")

        @Suppress("UNUSED_PARAMETER")
        fun cutInsideCandle(ignored: Candle, t1: Double, t2: Double): Candle = when {
            t1 == 0.0 && t2 == 1.0 -> cutCandle00to10
            t1 == 0.2 && t2 == 1.0 -> cutCandle02to10
            t1 == 0.0 && t2 == 0.8 -> cutCandle00to08
            t1 == 0.2 && t2 == 0.8 -> cutCandle02to08
            else -> unsupportedOperation()
        }

        val cutter = TimedCandleCutter(::cutInsideCandle)

        cutter.cut(timedCandle, instant(20)..instant(30)) shouldBe TimedCandle(instant(20)..instant(30), cutCandle00to10)
        cutter.cut(timedCandle, instant(22)..instant(30)) shouldBe TimedCandle(instant(22)..instant(30), cutCandle02to10)
        cutter.cut(timedCandle, instant(22)..instant(36)) shouldBe TimedCandle(instant(22)..instant(30), cutCandle02to10)
        cutter.cut(timedCandle, instant(20)..instant(28)) shouldBe TimedCandle(instant(20)..instant(28), cutCandle00to08)
        cutter.cut(timedCandle, instant(12)..instant(28)) shouldBe TimedCandle(instant(20)..instant(28), cutCandle00to08)
        cutter.cut(timedCandle, instant(22)..instant(28)) shouldBe TimedCandle(instant(22)..instant(28), cutCandle02to08)
    }
})