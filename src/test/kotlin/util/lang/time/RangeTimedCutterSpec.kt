package util.lang.time

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import util.lang.RangeTimed
import util.lang.unsupportedOperation
import java.time.Instant

class RangeTimedCutterSpec : FreeSpec({
    fun instant(millis: Int) = Instant.ofEpochMilli(millis.toLong())

    "cut timed" - {
        val cutCandle00to10 = Any()
        val cutCandle02to10 = Any()
        val cutCandle00to08 = Any()
        val cutCandle02to08 = Any()
        val cutCandleAtBegin = Any()
        val cutCandleAtEnd = Any()

        @Suppress("UNUSED_PARAMETER")
        fun cutInside(ignored: Any, t1: Double, t2: Double): Any = when {
            t1 < 0.0001 && t2 > 0.9999 -> cutCandle00to10
            t1 == 0.2 && t2 > 0.9999 -> cutCandle02to10
            t1 < 0.0001 && t2 == 0.8 -> cutCandle00to08
            t1 == 0.2 && t2 == 0.8 -> cutCandle02to08
            t1 < 0.0001 && t2 < 0.0001 -> cutCandleAtBegin
            t1 > 0.9999 && t2 > 0.9999 -> cutCandleAtEnd
            else -> unsupportedOperation()
        }

        val cutter = RangeTimedCutter(::cutInside)

        "cut small item" - {
            val timedCandle = RangeTimed(instant(20)..instant(30), Any())

            "cut whole" {
                cutter.cut(timedCandle, instant(20)..instant(30)) shouldBe RangeTimed(instant(20)..instant(30), cutCandle00to10)
            }

            "cut inside" {
                cutter.cut(timedCandle, instant(22)..instant(30)) shouldBe RangeTimed(instant(22)..instant(30), cutCandle02to10)
                cutter.cut(timedCandle, instant(20)..instant(28)) shouldBe RangeTimed(instant(20)..instant(28), cutCandle00to08)
                cutter.cut(timedCandle, instant(22)..instant(28)) shouldBe RangeTimed(instant(22)..instant(28), cutCandle02to08)
            }

            "cut intersect" {
                cutter.cut(timedCandle, instant(20)..instant(36)) shouldBe RangeTimed(instant(20)..instant(30), cutCandle00to10)
                cutter.cut(timedCandle, instant(22)..instant(36)) shouldBe RangeTimed(instant(22)..instant(30), cutCandle02to10)
                cutter.cut(timedCandle, instant(12)..instant(30)) shouldBe RangeTimed(instant(20)..instant(30), cutCandle00to10)
                cutter.cut(timedCandle, instant(12)..instant(28)) shouldBe RangeTimed(instant(20)..instant(28), cutCandle00to08)
                cutter.cut(timedCandle, instant(18)..instant(30)) shouldBe RangeTimed(instant(20)..instant(30), cutCandle00to10)
                cutter.cut(timedCandle, instant(10)..instant(40)) shouldBe RangeTimed(instant(20)..instant(30), cutCandle00to10)
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
                cutter.cut(timedCandle, Instant.MIN..instant(28)) shouldBe RangeTimed(instant(20)..instant(28), cutCandle00to08)
                cutter.cut(timedCandle, instant(22)..Instant.MAX) shouldBe RangeTimed(instant(22)..instant(30), cutCandle02to10)
                cutter.cut(timedCandle, Instant.MIN..Instant.MAX) shouldBe RangeTimed(instant(20)..instant(30), cutCandle00to10)
            }
        }

        "cut big item with min instant" - {
            val timedCandle = RangeTimed(Instant.MIN..instant(30), Any())

            "cut whole" {
                cutter.cut(timedCandle, Instant.MIN..instant(30)) shouldBe RangeTimed(Instant.MIN..instant(30), cutCandle00to10)
            }

            "cut inside" {
                cutter.cut(timedCandle, Instant.MIN..instant(28)) shouldBe RangeTimed(Instant.MIN..instant(28), cutCandle00to10)
            }

            "cut intersects" {
                cutter.cut(timedCandle, Instant.MIN..instant(32)) shouldBe RangeTimed(Instant.MIN..instant(30), cutCandle00to10)
            }

            "cut outside" {
                cutter.cut(timedCandle, instant(30)..instant(34)) shouldBe null
                cutter.cut(timedCandle, instant(31)..instant(34)) shouldBe null
                cutter.cut(timedCandle, instant(30)..Instant.MAX) shouldBe null
            }

            "cut small period" {
                cutter.cut(timedCandle, instant(20)..instant(28)) shouldBe RangeTimed(instant(20)..instant(28), cutCandleAtEnd)
                cutter.cut(timedCandle, instant(20)..instant(30)) shouldBe RangeTimed(instant(20)..instant(30), cutCandleAtEnd)
                cutter.cut(timedCandle, instant(20)..instant(36)) shouldBe RangeTimed(instant(20)..instant(30), cutCandleAtEnd)
            }

            "cut big period" {
                cutter.cut(timedCandle, Instant.MIN..Instant.MAX) shouldBe RangeTimed(Instant.MIN..instant(30), cutCandle00to10)
            }
        }

        "cut big item with max instant" - {
            val timedCandle = RangeTimed(instant(20)..Instant.MAX, Any())

            "cut whole" {
                cutter.cut(timedCandle, instant(20)..Instant.MAX) shouldBe RangeTimed(instant(20)..Instant.MAX, cutCandle00to10)
            }

            "cut inside" {
                cutter.cut(timedCandle, instant(22)..Instant.MAX) shouldBe RangeTimed(instant(22)..Instant.MAX, cutCandle00to10)
            }

            "cut intersects" {
                cutter.cut(timedCandle, instant(18)..Instant.MAX) shouldBe RangeTimed(instant(20)..Instant.MAX, cutCandle00to10)
            }

            "cut outside" {
                cutter.cut(timedCandle, instant(16)..instant(20)) shouldBe null
                cutter.cut(timedCandle, instant(16)..instant(19)) shouldBe null
                cutter.cut(timedCandle, Instant.MIN..instant(20)) shouldBe null
            }

            "cut small period" {
                cutter.cut(timedCandle, instant(22)..instant(30)) shouldBe RangeTimed(instant(22)..instant(30), cutCandleAtBegin)
                cutter.cut(timedCandle, instant(20)..instant(30)) shouldBe RangeTimed(instant(20)..instant(30), cutCandleAtBegin)
                cutter.cut(timedCandle, instant(14)..instant(24)) shouldBe RangeTimed(instant(20)..instant(24), cutCandleAtBegin)
            }

            "cut big period" {
                cutter.cut(timedCandle, Instant.MIN..Instant.MAX) shouldBe RangeTimed(instant(20)..Instant.MAX, cutCandle00to10)
            }
        }
    }
})