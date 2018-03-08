package old.exchange.candle

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.runBlocking
import com.dmi.util.lang.InstantRange
import java.math.BigDecimal
import java.time.Instant

class ContinuousCandlesSpec : FreeSpec({
    fun instant(millis: Int) = Instant.ofEpochMilli(millis.toLong())

    fun candle(open: String, close: String, high: String, low: String): Candle {
        return Candle(
                open = BigDecimal(open),
                close = BigDecimal(close),
                high = BigDecimal(high),
                low = BigDecimal(low)
        )
    }

    fun timedCandle(period: InstantRange, open: String, close: String, high: String, low: String) = TimedCandle(
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
})