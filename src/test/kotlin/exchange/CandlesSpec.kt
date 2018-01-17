package exchange

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import java.math.BigDecimal

class CandlesSpec : FreeSpec({
    "approximate candle with low is closer than high" {
        val candle = Candle(
                open = BigDecimal("10"),
                close = BigDecimal("12"),
                high = BigDecimal("16"),
                low = BigDecimal("8")
        )

        candle.approximate(0.0, operationScale = 2) shouldBe BigDecimal("10.00")
        candle.approximate(1.0, operationScale = 2) shouldBe BigDecimal("12.00")
        candle.approximate(1.0 / 3, operationScale = 2) shouldBe BigDecimal("8.00")
        candle.approximate(2.0 / 3, operationScale = 2) shouldBe BigDecimal("16.00")
        candle.approximate(0.5 / 3, operationScale = 2) shouldBe BigDecimal("9.00")
        candle.approximate(2.5 / 3, operationScale = 2) shouldBe BigDecimal("14.00")
    }

    "approximate candle with high is closer than low" {
        val candle = Candle(
                open = BigDecimal("10"),
                close = BigDecimal("12"),
                high = BigDecimal("14"),
                low = BigDecimal("4")
        )

        candle.approximate(0.0, operationScale = 2) shouldBe BigDecimal("10.00")
        candle.approximate(1.0, operationScale = 2) shouldBe BigDecimal("12.00")
        candle.approximate(1.0 / 3, operationScale = 2) shouldBe BigDecimal("14.00")
        candle.approximate(2.0 / 3, operationScale = 2) shouldBe BigDecimal("4.00")
        candle.approximate(0.5 / 3, operationScale = 2) shouldBe BigDecimal("12.00")
        candle.approximate(2.5 / 3, operationScale = 2) shouldBe BigDecimal("8.00")
    }
})