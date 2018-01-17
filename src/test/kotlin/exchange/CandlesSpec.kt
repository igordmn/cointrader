package exchange

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FreeSpec
import java.math.BigDecimal

class CandlesSpec : FreeSpec({
    "candle with low is closer than high" - {
        val candle = Candle(
                open = BigDecimal("10"),
                close = BigDecimal("12"),
                high = BigDecimal("16"),
                low = BigDecimal("8")
        )
        val approximatedPrices = LinearApproximatedPrices(candle, operationScale = 2)

        "approximate price" {
            approximatedPrices.exactAt(0.0 / 3) shouldBe BigDecimal("10.00")
            approximatedPrices.exactAt(0.5 / 3) shouldBe BigDecimal("9.00")
            approximatedPrices.exactAt(1.0 / 3) shouldBe BigDecimal("8.00")
            approximatedPrices.exactAt(1.5 / 3) shouldBe BigDecimal("12.00")
            approximatedPrices.exactAt(2.0 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.exactAt(2.5 / 3) shouldBe BigDecimal("14.00")
            approximatedPrices.exactAt(3.0 / 3) shouldBe BigDecimal("12.00")
        }

        "approximate high at extremums" {
            approximatedPrices.highBetween(0.0 / 3, 1.0 / 3) shouldBe BigDecimal("10.00")
            approximatedPrices.highBetween(1.0 / 3, 2.0 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.highBetween(2.0 / 3, 1.0 / 3) shouldBe BigDecimal("16.00")

            approximatedPrices.highBetween(0.0 / 3, 2.0 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.highBetween(1.0 / 3, 3.0 / 3) shouldBe BigDecimal("16.00")

            approximatedPrices.highBetween(0.0 / 3, 3.0 / 3) shouldBe BigDecimal("16.00")
        }

        "approximate high at mids" {
            approximatedPrices.highBetween(0.0 / 3, 0.5 / 3) shouldBe BigDecimal("10.00")
            approximatedPrices.highBetween(0.5 / 3, 1.0 / 3) shouldBe BigDecimal("9.00")
            approximatedPrices.highBetween(1.0 / 3, 1.5 / 3) shouldBe BigDecimal("12.00")
            approximatedPrices.highBetween(1.5 / 3, 2.0 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.highBetween(2.0 / 3, 2.5 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.highBetween(2.5 / 3, 3.0 / 3) shouldBe BigDecimal("14.00")

            approximatedPrices.highBetween(0.0 / 3, 1.5 / 3) shouldBe BigDecimal("12.00")
            approximatedPrices.highBetween(0.5 / 3, 2.0 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.highBetween(1.0 / 3, 2.5 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.highBetween(1.5 / 3, 3.0 / 3) shouldBe BigDecimal("16.00")

            approximatedPrices.highBetween(0.0 / 3, 2.5 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.highBetween(0.5 / 3, 3.0 / 3) shouldBe BigDecimal("16.00")
        }

        "approximate high at single points" {
            approximatedPrices.highBetween(0.0 / 3, 0.0 / 3) shouldBe BigDecimal("10.00")
            approximatedPrices.highBetween(0.5 / 3, 0.5 / 3) shouldBe BigDecimal("9.00")
            approximatedPrices.highBetween(1.0 / 3, 1.0 / 3) shouldBe BigDecimal("8.00")
            approximatedPrices.highBetween(1.5 / 3, 1.5 / 3) shouldBe BigDecimal("12.00")
            approximatedPrices.highBetween(2.0 / 3, 2.0 / 3) shouldBe BigDecimal("16.00")
            approximatedPrices.highBetween(2.5 / 3, 2.5 / 3) shouldBe BigDecimal("14.00")
            approximatedPrices.highBetween(3.0 / 3, 3.0 / 3) shouldBe BigDecimal("12.00")
        }
    }


    "candle with high is closer than low" - {
        val candle = Candle(
                open = BigDecimal("10"),
                close = BigDecimal("12"),
                high = BigDecimal("14"),
                low = BigDecimal("4")
        )
        val approximatedPrices = LinearApproximatedPrices(candle, operationScale = 2)

        "approximate price" {
            approximatedPrices.exactAt(0.0 / 3) shouldBe BigDecimal("10.00")
            approximatedPrices.exactAt(0.5 / 3) shouldBe BigDecimal("12.00")
            approximatedPrices.exactAt(1.0 / 3) shouldBe BigDecimal("14.00")
            approximatedPrices.exactAt(1.5 / 3) shouldBe BigDecimal("9.00")
            approximatedPrices.exactAt(2.0 / 3) shouldBe BigDecimal("4.00")
            approximatedPrices.exactAt(2.5 / 3) shouldBe BigDecimal("8.00")
            approximatedPrices.exactAt(3.0 / 3) shouldBe BigDecimal("12.00")
        }
    }
})